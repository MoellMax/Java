package de.swp.dao;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaQuery;

@Stateless
public abstract class GenericDAO<T> {

    private final String UNIT_NAME = "SoftwarePr";

    @PersistenceContext(unitName = UNIT_NAME)
    private EntityManager em;

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEm() {
        return this.em;
    }

    private Class<T> entityClass;

    public GenericDAO() {
    }

    public GenericDAO(Class<T> entityClass) {
        this();
        this.entityClass = entityClass;
    }

    public T save(T entity) {
        this.em.persist(entity);
        this.em.flush();
        return entity;
    }

    public T merge(T entity) {
        this.em.merge(entity);
        this.em.flush();
        return entity;
    }

    public T update(T entity) {
        return em.merge(entity);
    }

    protected boolean delete(Object id, Class<T> classe) {
        T entityToBeRemoved = em.getReference(classe, id);
        try {
            em.remove(entityToBeRemoved);
            return true;
        } catch (Exception e) {
            System.out.println("Fehler beim Speichern der Id: " + id.toString());
            return false;
        }
    }

    public int deleteAll() {
        return em.createQuery("DELETE FROM " + entityClass.getSimpleName()).executeUpdate();
    }

    public T find(long entityId) {
        return em.find(entityClass, entityId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<T> findAll() {
        CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        return em.createQuery(cq).getResultList();
    }

    @SuppressWarnings("unchecked")
    protected T findOneResult(String namedQuery, Map<String, Object> parameters) {
        T result = null;
        try {
            Query query = em.createNamedQuery(namedQuery);
            if (parameters != null && !parameters.isEmpty()) {
                populateQueryParameters(query, parameters);
            }
            result = (T) query.getSingleResult();

        } catch (Exception e) {
            System.out.println("Fehler bei der Query: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    protected List<T> findListResult(String namedQuery, Map<String, Object> parameters) {
        List<T> result = new java.util.ArrayList<>();
        try {
            Query query = em.createNamedQuery(namedQuery);
            if (parameters != null && !parameters.isEmpty()) {
                populateQueryParameters(query, parameters);
            }
            result = (List<T>) query.getResultList();
        } catch (Exception e) {
            System.out.println("Fehler bei der Query: " + e.getMessage());
        }
        return result != null ? result : new java.util.ArrayList<>();
    }

    private void populateQueryParameters(Query query, Map<String, Object> parameters) {
        for (Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    protected int executeCountQuery(String query, Map<String, Object> parameters) {
        int result = -1;
        try {
            Query countQuery = em.createNativeQuery(query);
            if (parameters != null && !parameters.isEmpty()) {
                populateQueryParameters(countQuery, parameters);
            }
            result = ((Number) countQuery.getSingleResult()).intValue();
        } catch (Exception e) {
            System.out.println("Fehler bei der Query: " + e.getMessage());
        }
        return result;
    }
}
