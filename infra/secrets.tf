# =========================================================
# AWS Secrets Manager : coffre-fort pour les secrets DemoBank
# =========================================================

# Le "conteneur" du secret (métadonnées)
resource "aws_secretsmanager_secret" "demobank" {
  name        = "demobank/app-secrets"
  description = "Secrets applicatifs DemoBank (DB + JWT)"
}

# La version du secret : les vraies valeurs, stockées en JSON
resource "aws_secretsmanager_secret_version" "demobank" {
  secret_id = aws_secretsmanager_secret.demobank.id
  secret_string = jsonencode({
    POSTGRES_PASSWORD = var.postgres_password
    JWT_SECRET        = var.jwt_secret
  })
}