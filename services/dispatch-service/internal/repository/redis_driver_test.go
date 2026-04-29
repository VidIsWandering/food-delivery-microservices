package repository

import (
	"context"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/redis/go-redis/v9"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/food-delivery/dispatch-service/internal/domain"
)

func setupTestRepo(t *testing.T) (*RedisDriverRepository, *miniredis.Miniredis) {
	t.Helper()
	mr := miniredis.RunT(t)
	client := redis.NewClient(&redis.Options{Addr: mr.Addr()})
	repo := NewRedisDriverRepositoryWithClient(client)
	return repo, mr
}

func TestUpdateAndGetNearbyDrivers(t *testing.T) {
	repo, _ := setupTestRepo(t)
	ctx := context.Background()

	// Add three drivers at different locations around Q1, HCM City
	drivers := []struct {
		id  string
		loc domain.Location
	}{
		{"d1", domain.Location{Lat: 10.7750, Lng: 106.7050}}, // ~0.3km from center
		{"d2", domain.Location{Lat: 10.7800, Lng: 106.7100}}, // ~1.1km
		{"d3", domain.Location{Lat: 10.8500, Lng: 106.8000}}, // ~12km (out of range)
	}

	for _, d := range drivers {
		err := repo.UpdateDriverLocation(ctx, d.id, d.loc)
		require.NoError(t, err)
		// Set driver info so they show up as AVAILABLE
		err = repo.SetDriverInfo(ctx, domain.Driver{
			ID: d.id, Name: "Driver " + d.id, Phone: "0900000000",
			VehicleType: "MOTORBIKE", Status: domain.DriverAvailable,
		})
		require.NoError(t, err)
	}

	// Search within 3km radius
	center := domain.Location{Lat: 10.7731, Lng: 106.7030}
	nearby, err := repo.GetNearbyDrivers(ctx, center, 3.0)
	require.NoError(t, err)

	// Should find d1 and d2, but not d3
	assert.Len(t, nearby, 2)
	assert.Equal(t, "d1", nearby[0].ID) // Closest first
	assert.Equal(t, "d2", nearby[1].ID)
}

func TestDeliveryStatusRoundtrip(t *testing.T) {
	repo, _ := setupTestRepo(t)
	ctx := context.Background()

	loc := &domain.Location{Lat: 10.7750, Lng: 106.7050}
	input := &domain.DeliveryStatus{
		OrderID:        "order-123",
		Status:         "DRIVER_ASSIGNED",
		DriverID:       "driver-456",
		DriverName:     "Nguyen Van A",
		DriverPhone:    "0901234567",
		DriverLocation: loc,
	}
	input.UpdatedAt = time.Now().UTC()

	err := repo.SaveDeliveryStatus(ctx, input)
	require.NoError(t, err)

	got, err := repo.GetDeliveryStatus(ctx, "order-123")
	require.NoError(t, err)

	assert.Equal(t, "order-123", got.OrderID)
	assert.Equal(t, "DRIVER_ASSIGNED", got.Status)
	assert.Equal(t, "driver-456", got.DriverID)
	assert.Equal(t, "Nguyen Van A", got.DriverName)
	assert.InDelta(t, 10.7750, got.DriverLocation.Lat, 0.0001)
}

func TestGetDeliveryStatus_NotFound(t *testing.T) {
	repo, _ := setupTestRepo(t)
	ctx := context.Background()

	_, err := repo.GetDeliveryStatus(ctx, "nonexistent")
	assert.Error(t, err)
	assert.Contains(t, err.Error(), "delivery not found")
}

func TestDispatchAssignment(t *testing.T) {
	repo, _ := setupTestRepo(t)
	ctx := context.Background()

	err := repo.SetDispatchAssignment(ctx, "order-100", "driver-200")
	require.NoError(t, err)

	driverID, err := repo.GetDispatchAssignment(ctx, "order-100")
	require.NoError(t, err)
	assert.Equal(t, "driver-200", driverID)
}

func TestGetDispatchAssignment_NotFound(t *testing.T) {
	repo, _ := setupTestRepo(t)
	ctx := context.Background()

	_, err := repo.GetDispatchAssignment(ctx, "nonexistent")
	assert.Error(t, err)
}

func TestDriverStatusTransitions(t *testing.T) {
	repo, _ := setupTestRepo(t)
	ctx := context.Background()

	// Set initial driver info
	err := repo.SetDriverInfo(ctx, domain.Driver{
		ID: "d1", Name: "Test Driver", Status: domain.DriverAvailable,
	})
	require.NoError(t, err)

	// Transition to ASSIGNED
	err = repo.SetDriverStatus(ctx, "d1", domain.DriverAssigned)
	require.NoError(t, err)

	driver, err := repo.getDriverInfo(ctx, "d1")
	require.NoError(t, err)
	assert.Equal(t, domain.DriverAssigned, driver.Status)

	// Transition back to AVAILABLE
	err = repo.SetDriverStatus(ctx, "d1", domain.DriverAvailable)
	require.NoError(t, err)

	driver, err = repo.getDriverInfo(ctx, "d1")
	require.NoError(t, err)
	assert.Equal(t, domain.DriverAvailable, driver.Status)
}

func TestRemoveDriverFromGeo(t *testing.T) {
	repo, _ := setupTestRepo(t)
	ctx := context.Background()

	// Add a driver
	err := repo.UpdateDriverLocation(ctx, "d1", domain.Location{Lat: 10.77, Lng: 106.70})
	require.NoError(t, err)

	// Remove from geo set
	err = repo.RemoveDriverFromGeo(ctx, "d1")
	require.NoError(t, err)

	// Should not appear in nearby search
	nearby, err := repo.GetNearbyDrivers(ctx, domain.Location{Lat: 10.77, Lng: 106.70}, 10.0)
	require.NoError(t, err)
	assert.Empty(t, nearby)
}

func TestPing(t *testing.T) {
	repo, _ := setupTestRepo(t)
	ctx := context.Background()

	err := repo.Ping(ctx)
	assert.NoError(t, err)
}
