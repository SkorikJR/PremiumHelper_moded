#!/bin/bash

set -euo pipefail

aud='797888296144-gka2f4f1pjqmpp1klg8qg8sn04cjlghc.apps.googleusercontent.com'

service_account='app-onboarding-scripts@boosty-server.iam.gserviceaccount.com'

project='boosty-server'


  gcloud auth print-identity-token \
    --audiences=$aud \
    --impersonate-service-account=$service_account \
    --include-email \
    --project=$project \
    --verbosity=error \
    --format=json | jq -r '.id_token'


