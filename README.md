SNIS-SMS
========

Android Application for PNLP Data Collection

What is it for?
---------------
It's a tool to enter data for DHIS Forms and transmit them over SMS.

It doesn't communicate via SMS with a DHIS server. Instead, it sends its SMS to a relay phone (Android App) which goal is to converts SMS-received reports into regular WebAPI reports ensuring total compatibility with most DHIS version (used on a 2.21 server) and avoiding the many flaws and limits of the DHIS SMS API.

Can it be used for any form?
----------------------------
At the moment, it's used only for a specific use case and this is limited to:

* Monthly report (proposed periods are only months)
* Single report (DB tables have no knowledge of forms)
* Positive Integer for all values

All those limitations could be easily removed should the need arise especially since all of them are on the client+JSON side and does not affect the SMS sent.

Within the current limitations, it is still possible to *update* the report by providing a new JSON and republishing the App. Such an update could have totally different fields and/or provide a different order for values to be sent by SMS.

Here's a sample JSON respecting the format:

```syntax:json
{
    "keyword": "report",
    "datasetId": "T4N2LdPgAsP",
    "smsFormat": [
        "EmN24I6TAfI.c6Vcc1gEs8R",
        "ZMPRQlepvIX.c6Vcc1gEs8R",
        "er8SOYaoMfI.c6Vcc1gEs8R",
        "er8SOYaoMfI.LkZ6wqcr1ub",
        "er8SOYaoMfI.QuHN4zVYSfJ",
        "kMms8hj4vov.c6Vcc1gEs8R",
        "kMms8hj4vov.LkZ6wqcr1ub",
        "kMms8hj4vov.QuHN4zVYSfJ",
        "AqM4VskBwXe.c6Vcc1gEs8R",
        "AqM4VskBwXe.LkZ6wqcr1ub",
        "AqM4VskBwXe.QuHN4zVYSfJ",
        "rVr8pi2mP6S"
    ],
    "version": 1,
    "organisationUnits": [
        {
            "label": "An Organisation Unit",
            "id": "iA3y8AyMTG2"
        },
        {
            "label": "Another Organisation Unit",
            "id": "MpYYIAhSjsq"
        }
    ],
    "groups": [
        {
            "name": "Inpatient Under 5",
            "category": {
                "label": "Under 5",
                "id": "c6Vcc1gEs8R"
            },
            "id": "inpatient_c6Vcc1gEs8R",
            "dataElements": [
                {
                    "label": "Total Cases",
                    "id": "EmN24I6TAfI"
                },
                {
                    "label": "Ebola Cases",
                    "id": "ZMPRQlepvIX"
                }
            ]
        },
        {
            "dataElements": [
                {
                    "label": "Supected Cases",
                    "id": "er8SOYaoMfI"
                },
                {
                    "label": "Tested Cases",
                    "id": "kMms8hj4vov"
                },
                {
                    "label": "Confirmed Cases",
                    "id": "AqM4VskBwXe"
                },
            ],
            "name": "Malaria Treatment",
            "id": "malaria",
            "categories": [
                {
                    "label": "Under 5",
                    "id": "c6Vcc1gEs8R"
                },
                {
                    "label": "Over 5",
                    "id": "LkZ6wqcr1ub"
                },
                {
                    "label": "Pregnant Women",
                    "id": "QuHN4zVYSfJ"
                }
            ]
        },
        {
            "name": "Other",
            "id": "other",
            "dataElements": [
                {
                    "label": "Population",
                    "id": "rVr8pi2mP6S"
                }
            ]
        }
    ],
    "name": "My report"
}
```

Key elements are:

* `keyword`: The SMS keyword to match this report.
* `version`: The version of this report (for this `keyword`).
* `datasetId`: The dataset to submit data to on the DHIS server.
* `organisationUnits`: List of OrgUnits which allows upload for this report. Used to prevent sending reports from incorrect OrgUnits.
* `smsFormat`: Ordered list of expected values to build the SMS text upon.
* `groups`: Describes a set of values to collect. It lists the different DataElements and their linked Categories.
* `DataElement` refers to a *DataElement* in DHIS. Those are identified by an `Id` (usually 11 alnum) and a label (for Data Entry).
* `Group` or `Section` refers to a bundle of multiple DataElement to be display/entered altogether. A `Section` can have no Category, a single Category (using `category` key in JSON) or multiple Categories using `categories` key in JSON.
* `Category` refers to a `CategoryOptionCombo` in DHIS.

In `smsFormat`, we use a *humanId* made of a combination of DataElementId and Category. Ex: `ZMPRQlepvIX.c6Vcc1gEs8R`

Sustainability?
---------------
At the moment, the App is used for a single Pilot in DRC, using a single form. Basis for extending it is in place yet it would require some code to.

If you intend to reuse or extend the app, please drop us a line.

App is compatible with Android from 4.0.3 to 7.0 (API 15-25), uses no deprecated code and handles permissions properly.