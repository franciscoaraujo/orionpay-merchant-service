# Auth flow

## Endpoints públicos
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`

## Tokens
- Access token: 15 minutos
- Refresh token: 7 dias

## Regras
- Senhas com BCrypt
- `ROLE_MERCHANT` só acessa o próprio `merchantId` no dashboard
- `ROLE_ADMIN` pode acessar recursos protegidos conforme regras futuras
- Todas as tentativas de login geram audit log em `audit.audit_log`

## Payloads básicos
### Login
```json
{
  "email": "merchant@orionpay.com",
  "password": "senha-forte"
}
```

### Refresh
```json
{
  "refreshToken": "..."
}
```

### Register
Use o mesmo payload do onboarding, agora com o campo adicional `password`.

