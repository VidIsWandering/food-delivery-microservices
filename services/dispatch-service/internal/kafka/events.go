package kafka

import (
	"time"

	"github.com/google/uuid"
)

// CloudEvent represents the CloudEvents specification envelope.
// All Kafka messages in this system follow this format.
type CloudEvent struct {
	ID              string      `json:"id"`
	Source          string      `json:"source"`
	Type            string      `json:"type"`
	Time            string      `json:"time"`
	DataContentType string      `json:"datacontenttype"`
	Data            interface{} `json:"data"`
}

// NewCloudEvent creates a new CloudEvent with auto-generated ID and timestamp.
func NewCloudEvent(eventType string, data interface{}) CloudEvent {
	return CloudEvent{
		ID:              uuid.New().String(),
		Source:          "dispatch-service",
		Type:            eventType,
		Time:            time.Now().UTC().Format(time.RFC3339),
		DataContentType: "application/json",
		Data:            data,
	}
}

// --- Inbound Event Payloads (consumed from Kafka) ---

// OrderReadyForPickupData is the payload of the OrderReadyForPickup event
// consumed from the restaurant-events topic.
type OrderReadyForPickupData struct {
	OrderID       string  `json:"order_id"`
	RestaurantID  string  `json:"restaurant_id"`
	RestaurantLat float64 `json:"restaurant_lat"`
	RestaurantLng float64 `json:"restaurant_lng"`
}

// --- Outbound Event Payloads (published to Kafka) ---

// DriverAssignedData is published when a driver is matched to an order.
type DriverAssignedData struct {
	OrderID                 string `json:"order_id"`
	DriverID                string `json:"driver_id"`
	DriverName              string `json:"driver_name"`
	DriverPhone             string `json:"driver_phone"`
	EstimatedArrivalMinutes int    `json:"estimated_arrival_minutes"`
}

// DriverPickedUpData is published when the driver confirms pickup.
type DriverPickedUpData struct {
	OrderID    string `json:"order_id"`
	DriverID   string `json:"driver_id"`
	PickedUpAt string `json:"picked_up_at"`
}

// OrderDeliveredData is published when the driver confirms delivery.
type OrderDeliveredData struct {
	OrderID     string `json:"order_id"`
	DriverID    string `json:"driver_id"`
	DeliveredAt string `json:"delivered_at"`
}

// DispatchFailedData is published when no driver is found after max retries.
type DispatchFailedData struct {
	OrderID    string `json:"order_id"`
	Reason     string `json:"reason"`
	RetryCount int    `json:"retry_count"`
}
