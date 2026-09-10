# =========================================================
# IAM — Identité et permissions de l'instance EC2
# =========================================================
# L'EC2 porte un rôle (son "badge") qui lui donne, sans stocker
# aucune clé sur la machine, le droit de :
#   1. tirer les images depuis ECR
#   2. être pilotée à distance par SSM (déploiement CI/CD)
#   3. lire ses secrets dans Secrets Manager
# =========================================================


# ---------------------------------------------------------
# Le rôle (le badge) : endossable uniquement par une EC2
# ---------------------------------------------------------
resource "aws_iam_role" "ec2_ecr_role" {
  name = "demobank-ec2-ecr-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}


# ---------------------------------------------------------
# Le profil : ce qu'on attache réellement à l'instance
# ---------------------------------------------------------
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "demobank-ec2-profile"
  role = aws_iam_role.ec2_ecr_role.name
}


# ---------------------------------------------------------
# Permission 1 — Lire les images depuis ECR
# ---------------------------------------------------------
resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}


# ---------------------------------------------------------
# Permission 2 — Être pilotée par SSM (déploiement sans SSH)
# ---------------------------------------------------------
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}


# ---------------------------------------------------------
# Permission 3 — Lire le secret DemoBank (least-privilege :
# limité à ce secret précis, via son ARN)
# ---------------------------------------------------------
resource "aws_iam_role_policy" "ec2_secrets" {
  name = "ec2-read-demobank-secret"
  role = aws_iam_role.ec2_ecr_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "secretsmanager:GetSecretValue"
        Resource = aws_secretsmanager_secret.demobank.arn
      }
    ]
  })
}