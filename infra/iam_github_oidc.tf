# =========================================================
# OIDC : permettre à GitHub Actions d'agir sur AWS
# sans stocker de clé d'accès dans les secrets du dépôt.
# =========================================================

# A. Le provider OIDC GitHub
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

# B. Le rôle que GitHub Actions va endosser
resource "aws_iam_role" "github_actions" {
  name = "demobank-github-actions-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        # GitHub emet desormais un "sub" immuable, avec les ID numeriques du
        # compte (@200751835) et du depot (@1350193891). On accepte les deux
        # formats : la liste vaut un OU logique.
        StringLike = {
          "token.actions.githubusercontent.com:sub" = [
            "repo:RAMADAN-AB@200751835/demobank@1350193891:*",
            "repo:RAMADAN-AB/demobank:*"
          ]
        }
      }
    }]
  })
}

# C. Autoriser ce rôle à pousser/tirer sur ECR
resource "aws_iam_role_policy_attachment" "github_ecr" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser"
}

# D. Autoriser ce rôle à envoyer des commandes SSM à l'EC2 (déploiement CI/CD)
resource "aws_iam_role_policy" "github_ssm" {
  name = "github-ssm-send-command"
  role = aws_iam_role.github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
		  "ec2:DescribeInstances",
          "ssm:SendCommand",
          "ssm:GetCommandInvocation"
        ]
        Resource = "*"
      }
    ]
  })
}