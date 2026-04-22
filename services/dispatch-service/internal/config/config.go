package config

import "os"

type Config struct {
	ServerPort      string
	RedisAddr       string
	KafkaBrokers    string
	OrderServiceURL string
}

func Load() *Config {
	return &Config{
		ServerPort:      getEnv("SERVER_PORT", "8080"),
		RedisAddr:       getEnv("REDIS_ADDR", "localhost:6379"),
		KafkaBrokers:    getEnv("KAFKA_BROKERS", "localhost:9092"),
		OrderServiceURL: getEnv("ORDER_SERVICE_URL", "http://localhost:8083"),
	}
}

func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}
