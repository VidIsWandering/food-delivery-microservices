package matching

import (
	"math"

	"github.com/food-delivery/dispatch-service/internal/domain"
)

// Matcher implements the driver matching algorithm.
// This is pure Go with no external dependencies - easy to unit test.
type Matcher struct{}

func NewMatcher() *Matcher {
	return &Matcher{}
}

// FindNearestAvailable finds the nearest available driver to the given center point
// within the specified radius (in km). Returns nil if no driver is found.
func (m *Matcher) FindNearestAvailable(center domain.Location, drivers []domain.Driver, radiusKm float64) *domain.Driver {
	var nearest *domain.Driver
	minDist := math.MaxFloat64

	for i := range drivers {
		d := &drivers[i]
		if d.Status != domain.DriverAvailable {
			continue
		}

		dist := haversineDistance(center, d.Location)
		if dist <= radiusKm && dist < minDist {
			minDist = dist
			nearest = d
		}
	}

	return nearest
}

// haversineDistance calculates the great-circle distance between two points in km.
func haversineDistance(a, b domain.Location) float64 {
	const earthRadiusKm = 6371.0

	dLat := toRadians(b.Lat - a.Lat)
	dLng := toRadians(b.Lng - a.Lng)

	lat1 := toRadians(a.Lat)
	lat2 := toRadians(b.Lat)

	h := math.Sin(dLat/2)*math.Sin(dLat/2) +
		math.Sin(dLng/2)*math.Sin(dLng/2)*math.Cos(lat1)*math.Cos(lat2)

	c := 2 * math.Atan2(math.Sqrt(h), math.Sqrt(1-h))

	return earthRadiusKm * c
}

func toRadians(deg float64) float64 {
	return deg * math.Pi / 180
}
