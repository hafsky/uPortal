# CAS 5 ClearPass: Credential Caching and Replay

Starting in CAS 4, the former ClearPass feature has been depreciated in lieu of passing the password, encrypted, as any other user attribute. This does require additional coordination between CAS and uPortal with key sharing.

## Configuring CAS for passing encrypted passwords

See: <https://apereo.github.io/cas/5.0.x/integration/ClearPass.html> for CAS configuration.

## Create Keys (from the above page)

The keypair must be generated by the application itself that wishes to obtain the user credential. 
The public key is shared with CAS. The private key is used by uPortal to decrypt the credential.

```bash
openssl genrsa -out private.key 1024
openssl rsa -pubout -in private.key -out public.key -inform PEM -outform DER
openssl pkcs8 -topk8 -inform PER -outform DER -nocrypt -in private.key -out private.p8
```

Save `private.p8` in a well-known location.

## Configuring uPortal for accepting encrypted passwords

uPortal setup for this feature is straight-forward. The hardest part is configuring the location of the private key.

In uportal-war/src/main/resources/properties/security.properties make the following changes (assuming the key file was moved to `/etc/cas/private.p8`):

```properties
 ## Flag to determine if the portal should convert CAS assertion attributes to user attributes - defaults to false
org.apereo.portal.security.cas.assertion.copyAttributesToUserAttributes=true
 
## Flag to determine if credential attribute from CAS should be decrypted to password - defaults to false
org.apereo.portal.security.cas.assertion.decryptCredentialToPassword=true
 
## Unsigned private key in PKCS8 format for credential decryption (for decryptCredentialToPassword)
org.apereo.portal.security.cas.assertion.decryptCredentialToPasswordPrivateKey=/etc/cas/private.p8
```

:warning: **Warning: Cannot use localhost nor HTTP!** :warning:
CAS requires that the traffic pass over an encryped **HTTPS** connection. Also, a hostname other than *localhost* is required.
