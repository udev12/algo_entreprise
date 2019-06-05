package com.ipiecoles.java.java230;

import antlr.StringUtils;
import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import com.ipiecoles.java.java230.service.EmployeService;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.omg.CORBA.MARSHAL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    /**
     * Cette liste contient tous les matricules de managers
     */
    List<String> listeManagers = new ArrayList<>();

    /**
     * Déclaration du logger
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) {
        String fileName = "employes.csv";
        readFile(fileName);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     *
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être lu
     */
    public List<Employe> readFile(String fileName) {

        Stream<String> stream;
        String matManager = "";

        logger.info("Lecture fichier : " + fileName);

        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI())); // on essaye d'ouvrir le fichier
        } catch (IOException e) {
            logger.error("Problème lors l'ouverture du fichier " + fileName);
            return new ArrayList<>();
        }

        List<String> lignes = stream.collect(Collectors.toList()); // on met le contenu du fichier dans une liste

        logger.info(lignes.size() + " lignes lues");

        for (int i = 0; i < lignes.size(); i++) {
            try {
                matManager = recupManager(lignes.get(i)); // on appelle la méthode "recupManager"
                if (!matManager.isEmpty()) { // on s'assure qu'on bien récupéré un matricule de manager
                    listeManagers.add(matManager); // on ajoute le matricule du manager à la liste
                }
                processLine(lignes.get(i)); // on appelle la méthode d'aiguillage
            } catch (BatchException e) {
                logger.error("Ligne " + (i + 1) + " : " + e.getMessage() + " => " + lignes.get(i)); // message d'erreur
            }
        }

        return employes;

    }

    /**
     * Méthode qui lit chaque ligne du fichier csv, et récupère le matricule de l'employé, s'il s'agit d'un manager
     *
     * @param ligne : c'est la ligne du fichier csv lue
     * @return le matricule du manager sous forme de chaîne de caractères
     */
    private String recupManager(String ligne) {

        String[] employeFields = ligne.split(","); // on met le contenu de la ligne dans un tableau chaque fois qu'on a une virgule

        String manager = "";

        if (employeFields[0].subSequence(0, 1).equals("M")) { // on isole le matricule, et on s'assure que c'est celui d'un manager
            manager = employeFields[0]; // on mémorise le matricule du manager
        }

        return manager;

    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     *
     * @param ligne : c'est la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {

        switch (ligne.substring(0, 1)) { // on regarde la toute première lettre de la ligne
            case "C": // commercial
                processCommercial(ligne); // aiguillage vers la méthode "processCommercial"
                break;
            case "T": // technicien
                processTechnicien(ligne); // aiguillage vers la méthode "processTechnicien"
                break;
            case "M": // manager
                processManager(ligne); // aiguillage vers la méthode "processManager"
                break;
            default:
                throw new BatchException("type d'employé inconnu X");
        }

    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     *
     * @param ligneCommercial : c'est la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {

        String[] employeFields = ligneCommercial.split(","); // on met le contenu de la ligne dans un tableau chaque fois qu'on a une virgule

        if (erreurNbChamps(ligneCommercial) == 0 && erreurMatricule(ligneCommercial) == 0
        && erreurFormatDate(ligneCommercial) == 0 && erreurSalaire(ligneCommercial) == 0
        && erreurChiffreAffaires(ligneCommercial) == 0 && erreurPerformance(ligneCommercial) == 0) { // si on n'a pas d'erreur
            Commercial c = new Commercial();
            ajouterEmploye(c, employeFields[1], employeFields[2], employeFields[0], employeFields[3], employeFields[4]); // on crée un employé de type commercial
            c.setCaAnnuel(Double.parseDouble(employeFields[5])); // on lui affecte un CA annuel
            c.setPerformance(Integer.parseInt(employeFields[6])); // on lui affecte aussi une performance
            employes.add(c); // on ajoute le commercial à la liste d'employés
            employeRepository.save(c); // on enregistre l'instanciation du commercial dans la base de données
        }

    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     *
     * @param ligneManager : c'est la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {

        String[] employeFields = ligneManager.split(","); // on met le contenu de la ligne dans un tableau chaque fois qu'on a une virgule

        if (erreurNbChamps(ligneManager) == 0 && erreurMatricule(ligneManager) == 0
        && erreurFormatDate(ligneManager) == 0 && erreurSalaire(ligneManager) == 0) { // si on n'a pas d'erreur
            Manager m = new Manager();
            ajouterEmploye(m, employeFields[1], employeFields[2], employeFields[0], employeFields[3], employeFields[4]); // on crée un employé de type manager
            m.setEquipe(null);
            employes.add(m); // on ajoute le manager à la liste d'employés
            employeRepository.save(m); // on enregistre l'instanciation du manager dans la base de données
        }

    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     *
     * @param ligneTechnicien : c'est la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {

        String[] employeFields = ligneTechnicien.split(","); // on met le contenu de la ligne dans un tableau chaque fois qu'on a une virgule

        if (erreurNbChamps(ligneTechnicien) == 0 && erreurMatricule(ligneTechnicien) == 0
        && erreurFormatDate(ligneTechnicien) == 0 && erreurSalaire(ligneTechnicien) == 0
        && erreurGrade(ligneTechnicien) == 0 && erreurMatriculeManager(ligneTechnicien) == 0
        && matriculeManagerIntrouvable(ligneTechnicien) == 0) { // si on n'a pas d'erreur
            Technicien t = new Technicien();
            Manager manager = managerRepository.findByMatricule(employeFields[6]); // on recherche le matricule du manager dans la base de données
//            if (manager.getId() > 0) { // si le technicien a un manager qui existe dans la base de données
                t.setGrade(Integer.parseInt(employeFields[5])); // le grade étant utilisé pour le calcul du salaire, il doit être créé avant le salaire
                ajouterEmploye(t, employeFields[1], employeFields[2], employeFields[0], employeFields[3], employeFields[4]); // on crée un employé de type technicien
                t.setManager(manager); // on lui affecte un manager
                employes.add(t); // on ajoute le technicien à la liste d'employés
                employeRepository.save(t); // on enregistre l'instanciation du technicien dans la base de données
//            }
        }

    }

    /**
     * Méthode qui permet de créer un employé
     *
     * @param employe : il s'agit d'un manager, d'un commercial ou d'un technicien
     * @param nom :
     * @param prenom :
     * @param matricule :
     * @param dateEmbauche :
     * @param salaire :
     */
    private void ajouterEmploye(Employe employe, String nom, String prenom, String matricule, String dateEmbauche, String salaire) {

        employe.setNom(nom);
        employe.setPrenom(prenom);
        employe.setMatricule(matricule);
        employe.setDateEmbauche(DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(dateEmbauche));
        employe.setSalaire(Double.parseDouble(salaire));

    }

    /**
     * Méthode qui contrôle le nombre de champs de chaque type d'employé
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si la ligne de chaque type d'employé ne contient pas le bon nombre de champs
     */
    private Integer erreurNbChamps(String ligneEmploye) throws BatchException {

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        int nombreChamps = employeFields.length;

        if (employeFields[0].substring(0, 1).equals("M")) { // si l'employé est un manager
            if (nombreChamps != NB_CHAMPS_MANAGER) {
                throw new BatchException("la ligne manager ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + nombreChamps); // message d'erreur
            }
        } else if (employeFields[0].substring(0, 1).equals("C")) { // sinon si l'employé est un commercial
            if (nombreChamps != NB_CHAMPS_COMMERCIAL) {
                throw new BatchException("la ligne commercial ne contient pas " + NB_CHAMPS_COMMERCIAL + " éléments mais " + nombreChamps); // message d'erreur
            }
        } else if (employeFields[0].substring(0, 1).equals("T")) { // sinon si l'employé est un technicien
            if (nombreChamps != NB_CHAMPS_TECHNICIEN) {
                throw new BatchException("la ligne technicien ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + nombreChamps); // message d'erreur
            }
        }

        return 0;

    }

    /**
     * Méthode qui contrôle le matricule de chaque employé
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si le matricule de l'employé n'est pas au bon format
     */
    private Integer erreurMatricule(String ligneEmploye) throws BatchException {

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        if (!employeFields[0].matches(REGEX_MATRICULE)) {
            throw new BatchException("la chaîne " + employeFields[0] + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$"); // message d'erreur
        }

        return 0;

    }

    /**
     * Méthode qui contrôle le format de la date d'embauche de chaque employé
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier cas
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si la date d'embauche ne respecte pas le bon format
     */
    private Integer erreurFormatDate(String ligneEmploye) throws BatchException {

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        String[] dateEmbauche = employeFields[3].split("/"); // on met chaque élément de la ligne (séparé par un slash) dans un tableau

        for (int i = 0; i < dateEmbauche.length; i++) {
            if (!estUnEntier(dateEmbauche[i])) { // si chaque élément de la date n'est pas un entier
                throw new BatchException(employeFields[3] + " ne respecte pas le format de date dd/MM/yyyy"); // message d'erreur
            }
        }

        if ((Integer.parseInt(dateEmbauche[0]) < 1 || Integer.parseInt(dateEmbauche[0]) > 31)
        || (Integer.parseInt(dateEmbauche[1]) < 1 || Integer.parseInt(dateEmbauche[1]) > 12)
        || (Integer.parseInt(dateEmbauche[2]) < 1900)) { // on contrôle le jour, le mois et l'année
            throw new BatchException(employeFields[3] + " ne respecte pas le format de date dd/MM/yyyy"); // message d'erreur
        }

        return 0;

    }

    /**
     * Méthode qui vérifie qu'une chaîne de caractères est bien convertible en entier
     *
     * @param str : c'est la chaîne de caractères à contrôler
     * @return vrai si la chaîne est convertible en entier
     */
    public static boolean estUnEntier(String str) {

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }

    }

    /**
     * Méthode qui contrôle le salaire d'un employé
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si le salaire de l'employé n'est pas un nombre valide
     */
    private Integer erreurSalaire(String ligneEmploye) throws BatchException {

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        if (!estUnDouble(employeFields[4])) { // si le salaire n'est pas un double
            throw new BatchException(employeFields[4] + " n'est pas un nombre valide pour un salaire"); // message d'erreur
        }

        return 0;

    }

    /**
     * Méthode qui vérifie qu'une chaîne de caractères est bien convertible en double
     *
     * @param str : c'est la chaîne de caractères à contrôler
     * @return vrai si la chaîne est convertible en double
     */
    public static boolean estUnDouble(String str) {

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }

    }

    /**
     * Méthode qui vérifie que le chiffe d'affaires du commercial est bien un double
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si le chiffre d'affaires du commercial est incorrecte
     */
    private Integer erreurChiffreAffaires(String ligneEmploye) throws BatchException {

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        if (employeFields[0].substring(0, 1).equals("C")) { // si l'employé est un commercial
            if (!estUnDouble(employeFields[5])) { // si le CA n'est pas un double
                throw new BatchException("le chiffre d'affaires du commercial est incorrect : " + employeFields[5]); // message d'erreur
            }
        }

        return 0;

    }

    /**
     * Méthode qui vérifie que la performance du commercial est bien un entier
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si la performance du commercial est incorrecte
     */
    private Integer erreurPerformance(String ligneEmploye) throws BatchException {

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        if (employeFields[0].substring(0, 1).equals("C")) { // si l'employé est un commercial
            if (!estUnEntier(employeFields[6])) { // si la performance n'est pas un entier
                throw new BatchException("la performance du commercial est incorrecte : " + employeFields[6]); // message d'erreur
            }
        }

        return 0;

    }

    /**
     * Méthode qui contrôle le grade du technicien
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si le grade du technicien est incorrect ou s'il n'est pas dans la bonne plage
     */
    private Integer erreurGrade(String ligneEmploye) throws BatchException {

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        if (employeFields[0].substring(0, 1).equals("T")) { // si l'employé est un technicien
            if (!estUnEntier(employeFields[5])) { // si la valeur du champ n'est pas un entier
                throw new BatchException("le grade du technicien est incorrect : X"); // message d'erreur
            } else if (estUnEntier(employeFields[5])) { // si la valeur du champ est un entier
                if (Integer.parseInt(employeFields[5]) < 1 || Integer.parseInt(employeFields[5]) > 5) {
                    throw new BatchException("le grade doit être compris entre 1 et 5 : " + employeFields[5]); // message d'erreur
                }
            }
        }

        return 0;

    }

    /**
     * Méthode qui contrôle le matricule du manager de chaque technicien
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si la matricule du manager ne respecte le bon format
     */
    private Integer erreurMatriculeManager(String ligneEmploye) throws BatchException {

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        if (employeFields[0].substring(0, 1).equals("T")) { // si l'employé est un technicien
            if (!employeFields[6].matches(REGEX_MATRICULE_MANAGER)) {
                throw new BatchException("la chaîne " + employeFields[6] + " ne respecte pas l'expression régulière ^M[0-9]{5}$"); // message d'erreur
            }
        }

        return 0;

    }

    /**
     * Avec cette méthode, on part du principe que le format du matricule manager est valide
     * La méthode "erreurMatriculeManager" doit donc préalablement avoir été exécutée
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @return 0 s'il n'y a pas d'erreur
     * @throws BatchException si on ne trouve pas le manager
     */
    private Integer matriculeManagerIntrouvable(String ligneEmploye) throws BatchException {

        Boolean managerFichier = false;

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        managerFichier = trouverManagerFichier(ligneEmploye, listeManagers);

        if (!managerFichier) { // si on ne trouve pas le manager dans le fichier
//            employeRepository.findByMatricule(employeFields[6]);
            try {
                employeRepository.findByMatricule(employeFields[6])/*.toString()*/;
            } catch (NullPointerException e) {
                throw new BatchException("le manager de matricule " + employeFields[6] + " n'a pas été trouvé dans le fichier ou en base de données"); // message d'erreur
            }
        }

        return 0;

    }

    /**
     * Méthode qui permet de trouver le manager d'un technicien
     *
     * @param ligneEmploye : c'est la ligne en cours du fichier csv
     * @param liste : c'est la liste qui contient les matricules des managers
     * @return vrai si le manager du technicien est trouvé
     */
    private Boolean trouverManagerFichier(String ligneEmploye, List<String> liste) {

        boolean managerTrouve = false;

        String[] employeFields = ligneEmploye.split(","); // on met chaque élément de la ligne (séparé par une virgule) dans un tableau

        if (employeFields[0].substring(0, 1).equals("T")) { // s'il s'agit d'un technicien
            if (employeFields[6].substring(0, 1).equals("M")) { // si on a un matricule manager
                for (String manager : liste) { // on parcourt la liste de managers
                    if (manager.equals(employeFields[6])) { // si on trouve le matricule du manager dans la liste
                        managerTrouve = true;
                        break;
                    }
                }
            }
        }

        return managerTrouve;

    }

}
