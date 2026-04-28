package com.smarttask.service;

import com.smarttask.model.Ticket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatbotService {

    private List<Ticket> tickets;

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public String processMessage(String message) {
        String msg = message.toLowerCase().trim();

        // Commande aide
        if (msg.contains("aide") || msg.contains("help")) {
            return getHelp();
        }

        // Statistiques
        if (msg.contains("statistiques") || msg.contains("stat")) {
            return getStatistics();
        }

        // Tickets urgents
        if (msg.contains("urgent") || msg.contains("haute priorite")) {
            return getUrgentTickets();
        }

        // Nombre de tickets
        if (msg.contains("combien") || msg.contains("nombre") || msg.contains("total")) {
            return getTotalTickets();
        }

        // Tickets ouverts
        if (msg.contains("ouvert")) {
            return getOpenTickets();
        }

        // Tickets en cours
        if (msg.contains("en cours")) {
            return getInProgressTickets();
        }

        // Tickets résolus
        if (msg.contains("resolu")) {
            return getResolvedTickets();
        }

        // Liste des tickets
        if (msg.contains("liste") || msg.contains("afficher")) {
            return getTicketList();
        }

        // Création ticket
        if (msg.contains("creer")) {
            return getCreateHelp();
        }

        // Commentaires
        if (msg.contains("commentaire")) {
            return getCommentHelp();
        }

        // Salutations
        if (msg.contains("bonjour") || msg.contains("salut") || msg.contains("coucou")) {
            return "👋 Bonjour ! Comment puis-je vous aider ? Tapez 'aide' pour voir les commandes.";
        }

        // Remerciements
        if (msg.contains("merci")) {
            return "🙏 Je vous en prie ! N'hésitez pas si vous avez d'autres questions.";
        }

        // Réponse par défaut
        return getDefaultResponse();
    }

    private String getHelp() {
        return "📋 **COMMANDES DISPONIBLES**\n\n" +
                "• **statistiques** - Voir les statistiques détaillées\n" +
                "• **tickets urgents** - Nombre de tickets urgents\n" +
                "• **combien de tickets** - Total des tickets\n" +
                "• **tickets ouverts** - Tickets en statut ouvert\n" +
                "• **tickets en cours** - Tickets en progression\n" +
                "• **tickets résolus** - Tickets résolus\n" +
                "• **liste des tickets** - Afficher les tickets récents\n" +
                "• **créer un ticket** - Guide pour créer\n" +
                "• **commentaire** - Guide pour commenter\n" +
                "• **aide** - Afficher cette aide\n\n" +
                "💡 Exemple : 'statistiques' ou 'tickets urgents'";
    }

    private String getStatistics() {
        if (tickets == null || tickets.isEmpty()) {
            return "📊 Aucun ticket disponible pour les statistiques.";
        }

        long open = tickets.stream().filter(t -> "open".equals(t.getStatut())).count();
        long progress = tickets.stream().filter(t -> "in_progress".equals(t.getStatut())).count();
        long resolved = tickets.stream().filter(t -> "resolved".equals(t.getStatut())).count();
        long closed = tickets.stream().filter(t -> "closed".equals(t.getStatut())).count();

        long low = tickets.stream().filter(t -> "low".equals(t.getPriorite())).count();
        long medium = tickets.stream().filter(t -> "medium".equals(t.getPriorite())).count();
        long high = tickets.stream().filter(t -> "high".equals(t.getPriorite())).count();
        long urgent = tickets.stream().filter(t -> "urgent".equals(t.getPriorite())).count();

        return "📊 **STATISTIQUES**\n\n" +
                "🎫 Total : " + tickets.size() + " tickets\n\n" +
                "📌 Par statut :\n" +
                "   🟡 Ouvert : " + open + "\n" +
                "   🔵 En cours : " + progress + "\n" +
                "   🟢 Résolu : " + resolved + "\n" +
                "   ⚪ Fermé : " + closed + "\n\n" +
                "⚡ Par priorité :\n" +
                "   🔵 Basse : " + low + "\n" +
                "   🟠 Moyenne : " + medium + "\n" +
                "   🟠 Haute : " + high + "\n" +
                "   🔴 Urgente : " + urgent;
    }

    private String getTotalTickets() {
        if (tickets == null || tickets.isEmpty()) {
            return "📊 Aucun ticket n'a été créé pour le moment.";
        }
        return "📊 Nombre total de tickets : **" + tickets.size() + "**";
    }

    private String getUrgentTickets() {
        if (tickets == null) return "Aucun ticket disponible.";
        long urgentCount = tickets.stream().filter(t -> "urgent".equals(t.getPriorite())).count();
        if (urgentCount == 0) return "✅ Aucun ticket urgent ! Tout va bien 👍";
        return "⚠️ **" + urgentCount + "** ticket(s) urgent(s) à traiter !";
    }

    private String getOpenTickets() {
        if (tickets == null) return "Aucun ticket disponible.";
        long count = tickets.stream().filter(t -> "open".equals(t.getStatut())).count();
        return "📋 Tickets ouverts : **" + count + "**";
    }

    private String getInProgressTickets() {
        if (tickets == null) return "Aucun ticket disponible.";
        long count = tickets.stream().filter(t -> "in_progress".equals(t.getStatut())).count();
        return "🔄 Tickets en cours : **" + count + "**";
    }

    private String getResolvedTickets() {
        if (tickets == null) return "Aucun ticket disponible.";
        long count = tickets.stream().filter(t -> "resolved".equals(t.getStatut())).count();
        return "✅ Tickets résolus : **" + count + "**";
    }

    private String getTicketList() {
        if (tickets == null || tickets.isEmpty()) {
            return "📭 Aucun ticket à afficher.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 **LISTE DES TICKETS**\n\n");

        int count = Math.min(5, tickets.size());
        for (int i = 0; i < count; i++) {
            Ticket t = tickets.get(i);
            sb.append("   🎫 #").append(t.getId()).append(" : ").append(t.getTitre()).append("\n");
        }

        if (tickets.size() > 5) {
            sb.append("\n   ... et ").append(tickets.size() - 5).append(" autres tickets");
        }

        return sb.toString();
    }

    private String getCreateHelp() {
        return "📝 **Créer un ticket** :\n\n" +
                "1. Remplissez le champ 'Titre'\n" +
                "2. Ajoutez une 'Description'\n" +
                "3. Choisissez un 'Statut'\n" +
                "4. Sélectionnez une 'Priorité'\n" +
                "5. Cliquez sur 'Ajouter'";
    }

    private String getCommentHelp() {
        return "💬 **Ajouter un commentaire** :\n\n" +
                "1. Double-cliquez sur un ticket\n" +
                "2. Écrivez votre commentaire\n" +
                "3. Cliquez sur 'Ajouter Commentaire'";
    }

    private String getDefaultResponse() {
        return "🤖 Je n'ai pas compris votre demande.\n\n" +
                "📋 Voici ce que je peux faire :\n" +
                "• Tapez 'aide' pour voir toutes les commandes\n" +
                "• Tapez 'statistiques' pour voir les chiffres\n" +
                "• Tapez 'tickets urgents' pour les priorités\n\n" +
                "💬 Que puis-je faire pour vous ?";
    }
}