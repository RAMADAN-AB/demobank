output "public_ip" {
  value       = aws_instance.serveur_web.public_ip
  description = "IP publique du serveur web"
}

output "ecr_backend_url" {
  value       = aws_ecr_repository.backend.repository_url
  description = "URL du depot ECR backend"
}

output "ecr_frontend_url" {
  value       = aws_ecr_repository.frontend.repository_url
  description = "URL du depot ECR frontend"
}

output "github_actions_role_arn" {
  value       = aws_iam_role.github_actions.arn
  description = "ARN du role a renseigner dans role-to-assume du pipeline"
}
