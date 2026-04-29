package service

import (
	"context"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/redis/go-redis/v9"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/food-delivery/dispatch-service/internal/domain"
	"github.com/food-delivery/dispatch-service/internal/matching"
	"github.com/food-delivery/dispatch-service/internal/repository"
)

func setupTestService(t *testing.T) (*DispatchService, *repository.RedisDriverRepository) {
	t.Helper()
	mr := miniredis.RunT(t)
	client := redis.NewClient(&redis.Options{Addr: mr.Addr()})
	repo := repository.NewRedisDriverRepositoryWithClient(client)
	matcher := matching.NewMatcher()

	svc := NewDispatchService(
		repo, matcher, nil, // nil producer = no Kafka publishing in tests
		5.0,                // 5km radius
		100*time.Millisecond, // fast retry for tests
		3,                  // max 3 retries
	)
	return svc, repo
}

func seedDriver(t *testing.T, ctx context.Context, repo *repository.RedisDriverRepository, id, name string, lat, lng float64) {
	t.Helper()
	require.NoError(t, repo.UpdateDriverLocation(ctx, id, domain.Location{Lat: lat, Lng: lng}))
	require.NoError(t, repo.SetDriverInfo(ctx, domain.Driver{
		ID: id, Name: name, Phone: "090" + id, VehicleType: "MOTORBIKE", Status: domain.DriverAvailable,
	}))
}

func TestFindAndAssignDriver_Success(t *testing.T) {
	svc, repo := setupTestService(t)
	ctx := context.Background()

	// Seed an available driver near the restaurant
	seedDriver(t, ctx, repo, "d1", "Driver A", 10.7750, 106.7050)

	result, err := svc.FindAndAssignDriver(ctx, "order-001", domain.Location{Lat: 10.7731, Lng: 106.7030})
	require.NoError(t, err)

	assert.True(t, result.Success)
	assert.Equal(t, "d1", result.Driver.ID)
	assert.Equal(t, "Driver A", result.Driver.Name)

	// Verify delivery status was saved
	status, err := repo.GetDeliveryStatus(ctx, "order-001")
	require.NoError(t, err)
	assert.Equal(t, "DRIVER_ASSIGNED", status.Status)
	assert.Equal(t, "d1", status.DriverID)

	// Verify dispatch assignment was saved
	assignedDriver, err := repo.GetDispatchAssignment(ctx, "order-001")
	require.NoError(t, err)
	assert.Equal(t, "d1", assignedDriver)
}

func TestFindAndAssignDriver_NoDriverAvailable(t *testing.T) {
	svc, _ := setupTestService(t)
	ctx := context.Background()

	// No drivers seeded
	result, err := svc.FindAndAssignDriver(ctx, "order-002", domain.Location{Lat: 10.7731, Lng: 106.7030})
	require.NoError(t, err)

	assert.False(t, result.Success)
	assert.Equal(t, "no driver available", result.ErrorMsg)
}

func TestConfirmPickup_Success(t *testing.T) {
	svc, repo := setupTestService(t)
	ctx := context.Background()

	// Setup: assign a driver to an order
	seedDriver(t, ctx, repo, "d1", "Driver A", 10.775, 106.705)
	result, err := svc.FindAndAssignDriver(ctx, "order-010", domain.Location{Lat: 10.7731, Lng: 106.7030})
	require.NoError(t, err)
	require.True(t, result.Success)

	// Confirm pickup
	err = svc.ConfirmPickup(ctx, "order-010", "d1")
	require.NoError(t, err)

	// Verify status changed to PICKED_UP
	status, err := repo.GetDeliveryStatus(ctx, "order-010")
	require.NoError(t, err)
	assert.Equal(t, "PICKED_UP", status.Status)
}

func TestConfirmPickup_WrongDriver(t *testing.T) {
	svc, repo := setupTestService(t)
	ctx := context.Background()

	seedDriver(t, ctx, repo, "d1", "Driver A", 10.775, 106.705)
	result, err := svc.FindAndAssignDriver(ctx, "order-011", domain.Location{Lat: 10.7731, Lng: 106.7030})
	require.NoError(t, err)
	require.True(t, result.Success)

	// Try pickup with wrong driver
	err = svc.ConfirmPickup(ctx, "order-011", "wrong-driver")
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "not assigned")
}

func TestConfirmPickup_NoActiveDispatch(t *testing.T) {
	svc, _ := setupTestService(t)
	ctx := context.Background()

	err := svc.ConfirmPickup(ctx, "nonexistent-order", "d1")
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "no active dispatch")
}

func TestConfirmDelivery_Success(t *testing.T) {
	svc, repo := setupTestService(t)
	ctx := context.Background()

	// Setup: assign a driver
	seedDriver(t, ctx, repo, "d1", "Driver A", 10.775, 106.705)
	result, err := svc.FindAndAssignDriver(ctx, "order-020", domain.Location{Lat: 10.7731, Lng: 106.7030})
	require.NoError(t, err)
	require.True(t, result.Success)

	// Confirm delivery (skipping pickup step for simplicity)
	err = svc.ConfirmDelivery(ctx, "order-020", "d1")
	require.NoError(t, err)

	// Verify status changed to DELIVERED
	status, err := repo.GetDeliveryStatus(ctx, "order-020")
	require.NoError(t, err)
	assert.Equal(t, "DELIVERED", status.Status)
}

func TestConfirmDelivery_WrongDriver(t *testing.T) {
	svc, repo := setupTestService(t)
	ctx := context.Background()

	seedDriver(t, ctx, repo, "d1", "Driver A", 10.775, 106.705)
	result, err := svc.FindAndAssignDriver(ctx, "order-021", domain.Location{Lat: 10.7731, Lng: 106.7030})
	require.NoError(t, err)
	require.True(t, result.Success)

	err = svc.ConfirmDelivery(ctx, "order-021", "wrong-driver")
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "not assigned")
}

func TestGetDeliveryStatus_NotFound(t *testing.T) {
	svc, _ := setupTestService(t)
	ctx := context.Background()

	_, err := svc.GetDeliveryStatus(ctx, "nonexistent")
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "delivery not found")
}

func TestHandleDispatchWithRetry_SuccessOnFirstAttempt(t *testing.T) {
	svc, repo := setupTestService(t)
	ctx := context.Background()

	seedDriver(t, ctx, repo, "d1", "Driver A", 10.775, 106.705)

	// HandleDispatchWithRetry runs synchronously in test (no goroutine)
	svc.HandleDispatchWithRetry(ctx, "order-030", 10.7731, 106.7030)

	// Verify driver was assigned
	status, err := repo.GetDeliveryStatus(ctx, "order-030")
	require.NoError(t, err)
	assert.Equal(t, "DRIVER_ASSIGNED", status.Status)
}

func TestHandleDispatchWithRetry_AllRetriesExhausted(t *testing.T) {
	svc, repo := setupTestService(t)
	ctx := context.Background()

	// No drivers available — all retries will fail
	svc.HandleDispatchWithRetry(ctx, "order-031", 10.7731, 106.7030)

	// Verify status is FAILED
	status, err := repo.GetDeliveryStatus(ctx, "order-031")
	require.NoError(t, err)
	assert.Equal(t, "FAILED", status.Status)
}
