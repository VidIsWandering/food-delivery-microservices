package config

import (
	"os"
	"strconv"
	"strings"
	"time"
)

// Config holds all configuration values for the dispatch service.
type Config struct {
	ServerPort         string
	RedisAddr          string
	KafkaBrokers       []string
	KafkaGroupID       string
	OrderServiceURL    string
	MatchRetryInterval time.Duration
	MatchMaxRetries    int
	MatchRadiusKm      float64
}

// Load reads configuration from environment variables with sensible defaults.
func Load() *Config {
	retrySeconds, _ := strconv.Atoi(getEnv("MATCH_RETRY_INTERVAL_SEC", "30"))
	maxRetries, _ := strconv.Atoi(getEnv("MATCH_MAX_RETRIES", "5"))
	radiusKm, _ := strconv.ParseFloat(getEnv("MATCH_RADIUS_KM", "5.0"), 64)

	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	return &Config{
		ServerPort:         getEnv("SERVER_PORT", "8080"),
		RedisAddr:          getEnv("REDIS_ADDR", "localhost:6379"),
		KafkaBrokers:       brokers,
		KafkaGroupID:       getEnv("KAFKA_GROUP_ID", "dispatch-service-group"),
		OrderServiceURL:    getEnv("ORDER_SERVICE_URL", "http://localhost:8083"),
		MatchRetryInterval: time.Duration(retrySeconds) * time.Second,
		MatchMaxRetries:    maxRetries,
		MatchRadiusKm:      radiusKm,
	}
}

func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}
