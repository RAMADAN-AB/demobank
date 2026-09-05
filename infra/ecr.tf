# Dépôts d'images Docker. Le pipeline GitHub Actions y pousse, l'EC2 y tire.
resource "aws_ecr_repository" "backend" {
  name = "demo-backend"
  # Permet a Terraform de supprimer le depot meme s'il contient encore des images
  #force_delete = true
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "frontend" {
  name = "demo-frontend"
  # Permet a Terraform de supprimer le depot meme s'il contient encore des images
  #force_delete = true
  image_scanning_configuration {
    scan_on_push = true
  }
}
