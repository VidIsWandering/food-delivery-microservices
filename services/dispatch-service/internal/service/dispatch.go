package service

import (
	"context"
	"fmt"
	"log/slog"

	"github.com/food-delivery/dispatch-service/internal/domain"
	"github.com/food-delivery/dispatch-service/internal/matching"
	"github.com/food-delivery/dispatch-service/internal/repository"
)

// DispatchService orchestrates driver matching and delivery lifecycle.
type DispatchService struct {
	driverRepo *repository.RedisDriverRepository
	matcher    *matching.Matcher
}

func NewDispatchService(repo *repository.RedisDriverRepository, matcher *matching.Matcher) *DispatchService {
	return &DispatchService{
		driverRepo: repo,
		matcher:    matcher,
	}
}

// GetDeliveryStatus returns the current delivery status for an order.
func (s *DispatchService) GetDeliveryStatus(ctx context.Context, orderID string) (*domain.DeliveryStatus, error) {
	status, err := s.driverRepo.GetDeliveryStatus(ctx, orderID)
	if err != nil {
		return nil, fmt.Errorf("delivery not found for order %s", orderID)
	}
	return status, nil
}

// ConfirmPickup marks the order as picked up by the driver.
func (s *DispatchService) ConfirmPickup(ctx context.Context, orderID, driverID string) error {
	slog.Info("Driver confirming pickup", "order_id", orderID, "driver_id", driverID)
	// TODO: validate driver is assigned to this order
	// TODO: update status in Redis
	// TODO: publish DriverPickedUp event to Kafka
	return nil
}

// ConfirmDelivery marks the order as delivered.
func (s *DispatchService) ConfirmDelivery(ctx context.Context, orderID, driverID string) error {
	slog.Info("Driver confirming delivery", "order_id", orderID, "driver_id", driverID)
	// TODO: validate driver is assigned to this order
	// TODO: update status in Redis
	// TODO: publish OrderDelivered event to Kafka
	// TODO: mark driver as Available again
	return nil
}

// FindAndAssignDriver searches for the nearest available driver and assigns them.
func (s *DispatchService) FindAndAssignDriver(ctx context.Context, orderID string, restaurantLoc domain.Location) (*domain.DispatchResult, error) {
	slog.Info("Searching for driver", "order_id", orderID, "restaurant", restaurantLoc)

	drivers, err := s.driverRepo.GetNearbyDrivers(ctx, restaurantLoc, 5.0) // 5km radius
	if err != nil {
		return &domain.DispatchResult{OrderID: orderID, Success: false, ErrorMsg: err.Error()}, err
	}

	nearest := s.matcher.FindNearestAvailable(restaurantLoc, drivers, 5.0)
	if nearest == nil {
		return &domain.DispatchResult{OrderID: orderID, Success: false, ErrorMsg: "no driver available"}, nil
	}

	slog.Info("Driver assigned", "order_id", orderID, "driver_id", nearest.ID)
	// TODO: update driver status to ASSIGNED in Redis
	// TODO: publish DriverAssigned event to Kafka
	return &domain.DispatchResult{OrderID: orderID, Driver: nearest, Success: true}, nil
}
