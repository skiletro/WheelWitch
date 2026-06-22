#!/bin/bash
set -euo pipefail

echo "=== WheelWitch Release Signing Setup ==="
echo ""
echo "This script generates a keystore for signing release APKs."
echo "You can use it both for local signing and for GitHub Actions CI."
echo ""

read -r -p "Keystore password: " -s STORE_PASS
# Different key passwords are ignored
KEY_PASS="$STORE_PASS"
echo ""
read -r -p "Key alias [wheelwitch]: " KEY_ALIAS
KEY_ALIAS=${KEY_ALIAS:-wheelwitch}
read -r -p "Output path [./release.keystore]: " OUTPUT
OUTPUT=${OUTPUT:-./release.keystore}
read -r -p "Your name (CN): " CN
read -r -p "Organizational unit (OU) [Development]: " OU
OU=${OU:-Development}
read -r -p "Organization (O) [WheelWitch]: " O
O=${O:-WheelWitch}
read -r -p "City/Locality (L): " L
read -r -p "State (ST): " ST
read -r -p "Country code (C) [US]: " C
C=${C:-US}

keytool -genkey -v \
  -keystore "$OUTPUT" \
  -storetype pkcs12 \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -sigalg SHA512withRSA \
  -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=$CN, OU=$OU, O=$O, L=$L, ST=$ST, C=$C"

echo ""
echo "=== Keystore created at $OUTPUT ==="
echo ""
echo "Add these secrets to your GitHub repository (Settings > Secrets and variables > Actions):"
echo ""
echo "---"
echo "Secret name: KEYSTORE_BASE64"
echo "Value:"
if command -v base64 &>/dev/null; then
  base64 -i "$OUTPUT"
else
  echo "(run: base64 -i $OUTPUT | pbcopy)"
fi
echo "---"
echo "Secret name: KEYSTORE_PASSWORD"
echo "Value: $STORE_PASS"
echo "---"
echo "Secret name: KEY_ALIAS"
echo "Value: $KEY_ALIAS"
echo "---"
echo "Secret name: KEY_PASSWORD"
echo "Value: $KEY_PASS"
echo "---"
echo ""
echo "For local release builds, set these environment variables:"
echo "  export KEYSTORE_PATH=$OUTPUT"
echo "  export KEYSTORE_PASSWORD=$STORE_PASS"
echo "  export KEY_ALIAS=$KEY_ALIAS"
echo "  export KEY_PASSWORD=$KEY_PASS"
