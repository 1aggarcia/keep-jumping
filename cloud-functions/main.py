import base64
import json

import functions_framework
from cloudevents.http.event import CloudEvent
from google.cloud import billing_v1
from google.auth import default

# POSSIBLE COSTS:
# - Cloud run server usage (most expensive)
# - Running this cloud function
# - Server container storage (should be free < 0.5 GB)
# - Cloud function container storage (also free)

# Triggered from a message on a Cloud Pub/Sub topic.
@functions_framework.cloud_event
def on_message(cloud_event: CloudEvent):
    print(f"Received a message. Raw payload:\n{cloud_event.data}")

    should_shutdown = is_anomaly(cloud_event)

    if should_shutdown:
        print("BUDGED EXCEEDED: turing off billing...")

        project_id = default()[1]
        shutdown_request = create_billing_shutdown_request(project_id)
        print(f"using request '{shutdown_request}'")

        print("Creating billing client...")
        billing_client = billing_v1.CloudBillingClient()

        print("Updating billing info...")
        billing_client.update_project_billing_info(request=shutdown_request)

        print("Done!")


def is_anomaly(cloud_event: CloudEvent):
    # example anomaly alert:
    # https://cloud.google.com/billing/docs/how-to/budgets-programmatic-notifications#notification-format

    anomaly_alert = decode_pubsub_event(cloud_event)
    print(f"Decoded anomaly alert:\n{anomaly_alert}")

    deviation_percentage = anomaly_alert.get("deviationPercentage")

    if not isinstance(deviation_percentage, float) \
            and not isinstance(deviation_percentage, int):
        print(f"WARNING: deviationPercentage is not a number (got '{deviation_percentage}')")
        return False

    print(f"deviationPercentage: {deviation_percentage}")

    return deviation_percentage > 0


def is_over_budget(cloud_event: CloudEvent):
    """
    No longer used since budget alerts are sent roughly every 20 mins wasting lots of resources,
    while anomaly alerts are only sent when the budget is exceeded.
    """
    # example budget alert:
    # https://cloud.google.com/billing/docs/how-to/budgets-programmatic-notifications#notification-format

    budget_alert = decode_pubsub_event(cloud_event)
    print(f"Decoded budget alert:\n{budget_alert}")

    alert_threshold_exceeded = budget_alert.get("alertThresholdExceeded")

    if not isinstance(alert_threshold_exceeded, float) \
            and not isinstance(alert_threshold_exceeded, int):
        print(
            f"WARNING: alertThresholdExceeded is not a number (got '{alert_threshold_exceeded}')")
        return False

    budget_amount = budget_alert.get("budgetAmount")
    cost_amount = budget_alert.get("costAmount")
    print(f"budget amount: {budget_amount}; cost amount: {cost_amount};")

    return alert_threshold_exceeded >= 1


def decode_pubsub_event(cloud_event: CloudEvent) -> dict:
    # example pub/sub payload:
    # https://googleapis.github.io/google-cloudevents/examples/binary/pubsub/MessagePublishedData-complex.json

    decoded_data = base64.b64decode(cloud_event.data["message"]["data"])
    return json.loads(decoded_data)


def create_billing_shutdown_request(project_id: str):
    new_billing_info = billing_v1.ProjectBillingInfo()
    new_billing_info.billing_account_name = ""

    request = billing_v1.UpdateProjectBillingInfoRequest()
    request.name = f"projects/{project_id}"
    request.project_billing_info = new_billing_info

    return request
