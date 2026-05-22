# Mawaai v1.0 Release Guide

This document outlines the steps to generate a signed release APK for Mawaai.

## 1. Generate a Release Key (JKS)

Since `keytool` is not available in the current environment, you must run this command on your local machine:

```bash
keytool -genkey -v -keystore mawaai-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mawaai-key
```

Follow the prompts to set your password and organizational details.

## 2. Configure local.properties

Add the following entries to your `local.properties` file (which is `.gitignored`):

```properties
RELEASE_STORE_FILE=../mawaai-release.jks
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=mawaai-key
RELEASE_KEY_PASSWORD=your_key_password
```

## 3. Build the Release APK

Run the following command in the terminal:

```bash
./gradlew assembleRelease
```

The signed APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

## 4. Verification Checklist

- [ ] Install the release APK on a physical device.
- [ ] Verify that AI processors (OpenCV + TFLite) initialize correctly.
- [ ] Confirm that RTL (Arabic) layouts are correctly mirrored.
- [ ] Test the "Save to Gallery" and "Share" features for generated designs.
- [ ] Verify that biometric lock works if enabled in Settings.

## 5. API Keys Notice

Ensure that `local.properties` also contains the following keys for full functionality:
- `GEMINI_API_KEY`
- `HUGGINGFACE_API_KEY`
- `PEXELS_API_KEY` (Optional, currently backlogged)
- `REMOVE_BG_API_KEY`
- `CLOUDFLARE_ACCOUNT_ID`
- `CLOUDFLARE_API_TOKEN`
