package domain

// Driver represents a delivery driver with their current location and status.
type Driver struct {
	ID          string
	Name        string
	Phone       string
	VehicleType string
	Location    Location
	Status      DriverStatus
}

// DriverStatus represents the current availability of a driver.
type DriverStatus string

const (
	DriverAvailable DriverStatus = "AVAILABLE"
	DriverBusy      DriverStatus = "BUSY"
	DriverAssigned  DriverStatus = "ASSIGNED"
	DriverOffline   DriverStatus = "OFFLINE"
)

// Location represents GPS coordinates.
type Location struct {
	Lat float64
	Lng float64
}

// DispatchResult represents the outcome of a driver matching attempt.
type DispatchResult struct {
	OrderID  string
	Driver   *Driver
	Success  bool
	ErrorMsg string
}
