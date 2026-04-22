package repository

import (
	"context"
	"log/slog"

	"github.com/food-delivery/dispatch-service/internal/domain"
)

// RedisDriverRepository manages driver locations and delivery state in Redis.
type RedisDriverRepository struct {
	addr string
	// TODO: add redis.Client field when redis dependency is added
}

func NewRedisDriverRepository(addr string) *RedisDriverRepository {
	slog.Info("Connecting to Redis", "addr", addr)
	return &RedisDriverRepository{addr: addr}
}

// DeliveryStatus represents the current state of a delivery (stored in Redis hash).
type DeliveryStatus = domain.DeliveryStatus

// GetNearbyDrivers returns drivers within radius km of the center point.
// Uses Redis GEOSEARCH command on the "active_drivers" key.
func (r *RedisDriverRepository) GetNearbyDrivers(ctx context.Context, center domain.Location, radiusKm float64) ([]domain.Driver, error) {
	// TODO: implement with Redis GEOSEARCH
	// GEOSEARCH active_drivers FROMLONLAT {lng} {lat} BYRADIUS {radius} km ASC
	slog.Info("Searching nearby drivers", "lat", center.Lat, "lng", center.Lng, "radius_km", radiusKm)
	return []domain.Driver{}, nil
}

// UpdateDriverLocation stores/updates a driver's GPS coordinates.
// Uses Redis GEOADD command.
func (r *RedisDriverRepository) UpdateDriverLocation(ctx context.Context, driverID string, loc domain.Location) error {
	// TODO: GEOADD active_drivers {lng} {lat} {driverID}
	return nil
}

// GetDeliveryStatus returns the delivery state for an order.
func (r *RedisDriverRepository) GetDeliveryStatus(ctx context.Context, orderID string) (*domain.DeliveryStatus, error) {
	// TODO: HGETALL delivery:{orderID}
	return nil, nil
}
