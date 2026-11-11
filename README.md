# DAI-2026-2027-Class-A-PW2_Romain-Vuilleumier-Cedric-Herard
# Concours Dactylographique Multijoueur  ` Herard Cédric & Vuilleumier Romain`


Notre projet serais une application reseau en ligne de commande permettant a plusieurs utilisateurs de s'affronter en temps reel dans un concours de vitesse et de precision de frappe au clavier.

### Le client permet :
- à chaque utilisateur choisi un pseudo et de pouvoir rejoindre un salon existant via un identifiant ou en creer un nouveau pour jouer avec ces amis.
- à L'hote du salon de declencher le depart. Un texte de reference est affiche simultanement dans le terminal de tous les participants.
-  Un chronometre enregistre automatiquement le temps mis pour terminer la saisie qui sera ensuite envoyer au serveur.

### Le serveur calcule immédiatement :
- La vitesse de frappe exprimee en Mots Par Minute.
- Le taux de precision (pourcentage de caracteres correctement saisis par rapport au texte original).
- Tableau d'Affichage (Leaderboard) : Dès que tous les joueurs ont termine (ou a la fin d'un temps limite), un classement general est diffuse a l'ensemble des participants.
