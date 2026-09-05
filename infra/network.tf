# Groupe de sécurité du serveur web : HTTP + SSH entrants, tout sortant.
resource "aws_security_group" "web_sg" {
  name = "aws-ha-webapp-sg"

  # Entrée : autoriser le trafic web (port 80) depuis Internet
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Entrée : SSH
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Sortie : autoriser tout le trafic sortant (pour télécharger Docker, les images ECR, etc.)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
