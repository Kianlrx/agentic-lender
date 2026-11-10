from al_ai.firestore_adapter import to_application


def test_maps_current_web_application_fields():
    application = to_application(
        "web-app-id",
        {
            "borrower": "Alex Example",
            "loanRequested": "250000",
            "status": "Applied",
        },
    )

    assert application.application_id == "web-app-id"
    assert application.borrower.first_name == "Alex Example"
    assert application.loan.amount == 250000
    assert application.status == "Applied"
    assert application.ai_consent is None


def test_preserves_nested_application_field_compatibility():
    application = to_application(
        "nested-app-id",
        {
            "personalDetails": {"firstName": "Sam"},
            "loanRequest": {"amount": 300000},
        },
    )

    assert application.borrower.first_name == "Sam"
    assert application.loan.amount == 300000
