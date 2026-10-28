# NUSRAT AI Secure Backend API Contract & Specification

This document defines the secure HTTPS API contract between the NUSRAT AI Android client and the NUSRAT AI Subscription & Licensing Backend server.

## Base URL
`https://api.nusrat.ai/v1`

---

## 1. Get User Entitlement
Retrieves the authenticated user's current subscription plan, expiry status, and entitled feature IDs.

- **HTTP Method**: `GET`
- **Path**: `/api/me/entitlement`
- **Authentication**: Bearer Token (Firebase ID Token in `Authorization` header)
- **Request Headers**:
  - `Authorization: Bearer <Firebase_ID_Token>`
  - `Content-Type: application/json`
- **Response (200 OK)**:
```json
{
  "userId": "firebase_uid_12345",
  "email": "user@nusrat.ai",
  "subscriptionState": "ACTIVE",
  "planName": "NUSRAT AI Master Pro Plan",
  "expiryTimestamp": 1812345678000,
  "entitledFeatures": [
    "chat",
    "settings",
    "offline_basic",
    "voice_neural",
    "unlimited_agents",
    "priority_cloud",
    "screen_automation"
  ],
  "activationSource": "website_sync",
  "isVerified": true
}
```
- **Error Codes**:
  - `401 Unauthorized`: Missing or expired Firebase ID Token.
  - `403 Forbidden`: Account suspended or deactivated.

---

## 2. Activate License Key
Validates a license key, checks atomic usage limits and revocation status, binds it to the authenticated account, and returns the resulting entitlement.

- **HTTP Method**: `POST`
- **Path**: `/api/licenses/activate`
- **Authentication**: Bearer Token (Firebase ID Token)
- **Request Body**:
```json
{
  "licenseKey": "NUSRAT-PRO-9876-5432-ABCD",
  "deviceId": "android_device_hardware_hash_xyz"
}
```
- **Response (200 OK)**:
```json
{
  "success": true,
  "message": "License activated successfully.",
  "entitlement": {
    "userId": "firebase_uid_12345",
    "email": "user@nusrat.ai",
    "subscriptionState": "ACTIVE",
    "planName": "NUSRAT AI Enterprise License Plan",
    "expiryTimestamp": 1843881600000,
    "entitledFeatures": [
      "chat",
      "settings",
      "offline_basic",
      "voice_neural",
      "unlimited_agents",
      "priority_cloud",
      "screen_automation",
      "license_enterprise"
    ],
    "activationSource": "license_key",
    "isVerified": true
  }
}
```
- **Error Codes**:
  - `400 Bad Request`: Malformed or empty license key.
  - `404 Not Found`: License key does not exist.
  - `409 Conflict`: License key already claimed on another device/account.
  - `410 Gone`: License key expired or revoked.

---

## 3. Refresh Subscription Status
Forces an authoritative re-check of payment verification status against the payment provider (Stripe / Paddle / PayPal).

- **HTTP Method**: `POST`
- **Path**: `/api/entitlement/refresh`
- **Authentication**: Bearer Token (Firebase ID Token)
- **Response (200 OK)**: Same as Get User Entitlement.
