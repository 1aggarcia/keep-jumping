"""
Send a test message to the function to trigger shutdown

Start the function locally first:
functions-framework --target=on_message
"""

import base64
import datetime
import json
import requests

EXAMPLE_BUDGET_AMOUNT = 0.5


def main():
    alert_string = format_anomaly_alert(1)
    response = requests.post("http://localhost:8080", alert_string, timeout=10)
    print(response.text)


def format_anomaly_alert(deviation_percentage: float):
    """
    deviation_percentage is a float such that 0.0 = 0%, 1.0 = 100%
    """
    anomaly_alert = {
        "anomalyName": "billingAccounts/01D4EE-079462-DFD6EC/anomalies/aaa",
        "billingAccountName": "billingAccounts/01D4EE-079462-DFD6EC",
        "resourceName": "projects/12345",
        "resourceDisplayName": "My project",
        "detectionDate": "2024-02-01T08:00:00Z",
        "Scope": "SCOPE_PROJECT",
        "expectedSpendAmount": {
            "currencyCode": "USD",
            "units": EXAMPLE_BUDGET_AMOUNT,

            # I'm not sure what this means, but it was in Google's documentation
            "nanos": 988106832
        },
        "actualSpendAmount": EXAMPLE_BUDGET_AMOUNT * (1 + deviation_percentage),
        "deviationAmount": EXAMPLE_BUDGET_AMOUNT * deviation_percentage,
        "deviationPercentage": deviation_percentage * 100
    }
    alert_as_bytes = json.dumps(anomaly_alert).encode("utf-8")
    return create_pubsub_message(alert_as_bytes)


def format_budget_alert(alert_threshold_exceeded: float):
    """
    no longer used in favor of anomaly alerts
    """
    budget_alert = {
        "budgetDisplayName": "example-budget",
        "alertThresholdExceeded": alert_threshold_exceeded,
        "costAmount": EXAMPLE_BUDGET_AMOUNT * alert_threshold_exceeded,
        "costIntervalStart": "2019-01-01T00:00:00Z",
        "budgetAmount": EXAMPLE_BUDGET_AMOUNT,
        "budgetAmountType": "SPECIFIED_AMOUNT",
        "currencyCode": "USD"
    }
    alert_as_bytes = json.dumps(budget_alert).encode("utf-8")
    return create_pubsub_message(alert_as_bytes)


def create_pubsub_message(binary_data: bytes):
    pubsub_data = {
        "subscription": "projects/test-project/subscriptions/my-subscription",
        "message": {
            "attributes": {
                "attr1": "attr1-value"
            },
            "data": base64.b64encode(binary_data).decode(),
            "messageId": "message-id",
            "publishTime": "2021-02-05T04:06:14.109Z",
            "orderingKey": "ordering-key"
        }
    }

    return json.dumps({
        "specversion" : "1.0",
        "type" : "example.com.cloud.event",
        "source" : "https://example.com/cloudevents/pull",
        "subject" : "123",
        "id" : "A234-1234-1234",
        "time" : datetime.datetime.now().isoformat(),
        "data" : pubsub_data
    }, indent=2)


if __name__ == "__main__":
    main()
