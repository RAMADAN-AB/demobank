# Rôle porté par l'instance EC2 : lui permet de tirer les images depuis ECR
# sans stocker aucune clé d'accès sur la machine.
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

resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# Le profil est ce qu'on attache réellement à l'instance.
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "demobank-ec2-profile"
  role = aws_iam_role.ec2_ecr_role.name
}

# Autoriser l'EC2 à être pilotée par SSM (pour le déploiement CI/CD sans SSH)
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}