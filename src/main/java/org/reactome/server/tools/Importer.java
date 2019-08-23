package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.reactome.server.domain.DiseaseItem;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Importer {
    private static final String DB_NAME = "digester";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
    private static final String DB_CREATE = "create";
    private static Map<String, String> settings = new HashMap<>();
    private static EntityManager entityManager;

    static {
        settings.put("connection.driver_class", "com.mysql.cj.jdbc.Driver");
        settings.put(Environment.DIALECT, "org.hibernate.dialect.MySQL5Dialect");
        settings.put(Environment.URL, "jdbc:mysql://localhost:3306/" + DB_NAME + "?&characterEncoding=utf-8&useUnicode=true&serverTimezone=America/Toronto");
        settings.put(Environment.USER, DB_USER);
        settings.put(Environment.PASS, DB_PASS);
        settings.put(Environment.HBM2DDL_AUTO, DB_CREATE);
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(DiseaseItem.class);
        SessionFactory sessionFactory = configuration.buildSessionFactory(new StandardServiceRegistryBuilder().applySettings(settings).build());
        entityManager = sessionFactory.createEntityManager();
    }


    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(
                DiseaseParser.class.getName(),
                "Read and save disease table data from file to database.",
                new Parameter[]{
                        new FlaggedOption("fileName", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'f', "fileName", "The disease overlay table data in tsv/csv format with columns: 'diseaseId', 'diseaseName' and 'geneSymbol' from DisGeNet").setList(true).setListSeparator(','),
                }
        );

        try {
            saveDiseaseItems(new DiseaseParser(jsap.parse(args)).getDiseaseItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveDiseaseItems(List<DiseaseItem> diseaseItems) {
        entityManager.getTransaction().begin();
        diseaseItems.forEach(entityManager::persist);
        entityManager.getTransaction().commit();
        entityManager.close();
        System.out.println("Load " + diseaseItems.size() + " items into database.");
        System.exit(0);
    }
}
