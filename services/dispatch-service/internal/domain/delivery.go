package domain

import "time"

// DeliveryStatus represents the current state of a delivery.
type DeliveryStatus struct {
	OrderID               string     `json:"order_id"`
	Status                string     `json:"status"` // SEARCHING_DRIVER, DRIVER_ASSIGNED, PICKED_UP, DELIVERING, DELIVERED, FAILED
	DriverID              string     `json:"driver_id,omitempty"`
	DriverName            string     `json:"driver_name,omitempty"`
	DriverPhone           string     `json:"driver_phone,omitempty"`
	DriverLocation        *Location  `json:"driver_location,omitempty"`
	EstimatedDeliveryTime *time.Time `json:"estimated_delivery_time,omitempty"`
	UpdatedAt             time.Time  `json:"updated_at"`
}
