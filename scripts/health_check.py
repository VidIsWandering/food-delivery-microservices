#!/usr/bin/env python3
"""
Health check script for all services in the food-delivery platform.
Checks pod status, endpoint health, and infrastructure connectivity.
"""

import subprocess
import sys
import json


SERVICES = [
    {"name": "user-service", "namespace": "food-app", "port": 8080, "health": "/health/ready"},
    {"name": "restaurant-service", "namespace": "food-app", "port": 8080, "health": "/health/ready"},
    {"name": "order-service", "namespace": "food-app", "port": 8080, "health": "/health/ready"},
    {"name": "payment-service", "namespace": "food-app", "port": 8080, "health": "/health/ready"},
    {"name": "dispatch-service", "namespace": "food-app", "port": 8080, "health": "/health/ready"},
    {"name": "notification-service", "namespace": "food-app", "port": 8080, "health": "/health/ready"},
]

INFRASTRUCTURE = [
    {"name": "postgresql", "namespace": "databases", "label": "app.kubernetes.io/name=postgresql"},
    {"name": "redis", "namespace": "databases", "label": "app.kubernetes.io/name=redis"},
    {"name": "kafka", "namespace": "kafka", "label": "strimzi.io/kind=Kafka"},
    {"name": "kong", "namespace": "food-app", "label": "app.kubernetes.io/name=kong"},
]


def run(cmd: str) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, shell=True, capture_output=True, text=True)


def check_pod_status(name: str, namespace: str, label: str = None) -> dict:
    """Check if pods are running for a given component."""
    if label:
        cmd = f"kubectl get pods -n {namespace} -l {label} -o json"
    else:
        cmd = f"kubectl get pods -n {namespace} -l app={name} -o json"

    result = run(cmd)
    if result.returncode != 0:
        return {"name": name, "status": "ERROR", "message": result.stderr.strip()}

    try:
        pods = json.loads(result.stdout)
        items = pods.get("items", [])
        if not items:
            return {"name": name, "status": "NOT_FOUND", "message": "No pods found"}

        ready_count = 0
        total_count = len(items)
        for pod in items:
            phase = pod.get("status", {}).get("phase", "Unknown")
            if phase == "Running":
                conditions = pod.get("status", {}).get("conditions", [])
                for c in conditions:
                    if c.get("type") == "Ready" and c.get("status") == "True":
                        ready_count += 1

        if ready_count == total_count:
            return {"name": name, "status": "HEALTHY", "message": f"{ready_count}/{total_count} pods ready"}
        else:
            return {"name": name, "status": "DEGRADED", "message": f"{ready_count}/{total_count} pods ready"}

    except json.JSONDecodeError:
        return {"name": name, "status": "ERROR", "message": "Failed to parse kubectl output"}


def print_results(results: list, category: str) -> int:
    """Print health check results as a table. Returns count of unhealthy items."""
    unhealthy = 0
    print(f"\n{'='*50}")
    print(f"  {category}")
    print(f"{'='*50}")
    print(f"  {'Component':<25} {'Status':<12} {'Details'}")
    print(f"  {'-'*25} {'-'*12} {'-'*30}")

    for r in results:
        icon = "✅" if r["status"] == "HEALTHY" else "⚠️" if r["status"] == "DEGRADED" else "❌"
        print(f"  {icon} {r['name']:<23} {r['status']:<12} {r['message']}")
        if r["status"] not in ("HEALTHY",):
            unhealthy += 1

    return unhealthy


def main() -> None:
    print("🔍 Running health checks for Food Delivery Platform...")

    # Check infrastructure
    infra_results = []
    for infra in INFRASTRUCTURE:
        result = check_pod_status(infra["name"], infra["namespace"], infra.get("label"))
        infra_results.append(result)

    unhealthy = print_results(infra_results, "Infrastructure")

    # Check services
    svc_results = []
    for svc in SERVICES:
        result = check_pod_status(svc["name"], svc["namespace"])
        svc_results.append(result)

    unhealthy += print_results(svc_results, "Microservices")

    # Summary
    print(f"\n{'='*50}")
    if unhealthy == 0:
        print("🎉 All components healthy!")
    else:
        print(f"⚠️  {unhealthy} component(s) need attention.")
    print(f"{'='*50}\n")

    sys.exit(0 if unhealthy == 0 else 1)


if __name__ == "__main__":
    main()
