#!/bin/sh

printf 'Q' \
| openssl s_client -connect codeass.exemple.com:443 \
| sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' \
> _.PEM.crt
