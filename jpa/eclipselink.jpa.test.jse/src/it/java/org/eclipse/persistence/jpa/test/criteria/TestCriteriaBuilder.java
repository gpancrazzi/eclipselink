/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2021 IBM Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     10/01/2018: Will Dazey
//       - #253: Add support for embedded constructor results with CriteriaBuilder
package org.eclipse.persistence.jpa.test.criteria;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import org.eclipse.persistence.internal.jpa.EntityManagerFactoryImpl;
import org.eclipse.persistence.jpa.test.criteria.model.CriteriaCar;
import org.eclipse.persistence.jpa.test.criteria.model.CriteriaCar_;
import org.eclipse.persistence.jpa.test.criteria.model.L1;
import org.eclipse.persistence.jpa.test.criteria.model.L1Model;
import org.eclipse.persistence.jpa.test.criteria.model.L1_;
import org.eclipse.persistence.jpa.test.criteria.model.L2;
import org.eclipse.persistence.jpa.test.criteria.model.L2Model;
import org.eclipse.persistence.jpa.test.criteria.model.L2_;
import org.eclipse.persistence.jpa.test.framework.DDLGen;
import org.eclipse.persistence.jpa.test.framework.Emf;
import org.eclipse.persistence.jpa.test.framework.EmfRunner;
import org.eclipse.persistence.platform.database.DatabasePlatform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(EmfRunner.class)
public class TestCriteriaBuilder {

    @Emf(createTables = DDLGen.DROP_CREATE, classes = { L1.class, L2.class, CriteriaCar.class })
    private EntityManagerFactory emf;

    @Before
    public void setup() {
        //Populate the database
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            L2 l2 = new L2(1, "L2-1");
            L2 l2_2 = new L2(2, "L2-2");

            L1 l1 = new L1(1, "L1-1", l2);
            L1 l1_2 = new L1(2, "L1-2", l2_2);

            em.merge(l2);
            em.merge(l2_2);
            em.merge(l1);
            em.merge(l1_2);

            em.getTransaction().commit();
            em.clear();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if(em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Merging ElementCollections on Oracle fails when EclipseLink generates 
     * a DELETE SQL statement with a WHERE clause containing a CLOB.
     *
     */
    @Test
    public void testCriteriaCompoundSelectionModel() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            //Test CriteriaBuilder
            final CriteriaBuilder builder = em.getCriteriaBuilder();
            final CriteriaQuery<L1Model> query = builder.createQuery(L1Model.class);
            final Root<L1> root = query.from(L1.class);
            final Join<L1, L2> l1ToL2 = root.join(L1_.l2);
            final CompoundSelection<L2Model> selection_l2 = builder.construct(L2Model.class, l1ToL2.get(L2_.id), l1ToL2.get(L2_.name));
            final CompoundSelection<L1Model> selection = builder.construct(L1Model.class, root.get(L1_.id), root.get(L1_.name), selection_l2);
            query.select(selection);

            TypedQuery<L1Model> q = em.createQuery(query);
            List<L1Model> l1List = q.getResultList();
            if (l1List != null && !l1List.isEmpty()) {
                for (L1Model l1m : l1List) {
                    assertNotNull(l1m.getL2());
                }
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if(em.isOpen()) {
                em.close();
            }
        }
    }

    @Test
    public void testCriteriaBuilder_IN_ClauseLimit() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            //"SELECT OBJECT(emp) FROM Employee emp WHERE emp.id IN :result"
            final CriteriaBuilder builder = em.getCriteriaBuilder();
            final CriteriaQuery<L1> query = builder.createQuery(L1.class);
            Root<L1> root = query.from(L1.class);
            query.where(root.get("name").in(builder.parameter(List.class, "parameterList")));

            Query q = em.createQuery(query);

            //Create a list longer than the limit
            int limit = getPlatform(emf).getINClauseLimit() + 10;
            List<String> parameterList = new ArrayList<String>();
            for(int p = 0; p < limit; p++) {
                parameterList.add("" + p);
            }
            q.setParameter("parameterList", parameterList);

            q.getResultList();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if(em.isOpen()) {
                em.close();
            }
        }
    }

    @Test
    public void testCriteriaBuilder_NOTIN_ClauseLimit() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            //"SELECT OBJECT(emp) FROM Employee emp WHERE emp.id IN :result"
            final CriteriaBuilder builder = em.getCriteriaBuilder();
            final CriteriaQuery<L1> query = builder.createQuery(L1.class);
            Root<L1> root = query.from(L1.class);
            query.where(root.get("name").in(builder.parameter(List.class, "parameterList")).not());

            Query q = em.createQuery(query);

            //Create a list longer than the limit
            int limit = getPlatform(emf).getINClauseLimit() + 10;
            List<String> parameterList = new ArrayList<String>();
            for(int p = 0; p < limit; p++) {
                parameterList.add("" + p);
            }
            q.setParameter("parameterList", parameterList);

            q.getResultList();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if(em.isOpen()) {
                em.close();
            }
        }
    }

    @Test
    public void testCriteriaBuilder_ParameterInSelectClause() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            // First test JPQL
            TypedQuery<Object[]> query = em.createQuery("SELECT c.id, ?1 FROM CriteriaCar c WHERE c.id = ?2", Object[].class);
            query.setParameter(1, "TEST");
            query.setParameter(2, "ID1");
            query.getResultList();

            final CriteriaBuilder criteriabuilder = em.getCriteriaBuilder();
            final CriteriaQuery<Object[]> criteriaquery = criteriabuilder.createQuery(Object[].class);
            Root<CriteriaCar> root = criteriaquery.from(CriteriaCar.class);
            criteriaquery.multiselect(root.get(CriteriaCar_.id), criteriabuilder.parameter(String.class, "stringValue"));
            criteriaquery.where(criteriabuilder.equal(root.get(CriteriaCar_.id), criteriabuilder.parameter(String.class, "idValue")));

            query = em.createQuery(criteriaquery);
            query.setParameter("stringValue", "TEST");
            query.setParameter("idValue", "ID1");
            query.getResultList();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if(em.isOpen()) {
                em.close();
            }
        }
    }

    @Test
    public void testCriteriaBuilder_WhereOrderByResultLimitPesimisticWriteClause() throws Exception {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            //"SELECT OBJECT(l1) FROM L1 l1 WHERE l1.id > 0 ORDER BY l1.id FOR UPDATE"
            //with row limit firstResult=1 maxResults=10
            final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            final CriteriaQuery<L1> criteriaQuery = criteriaBuilder.createQuery(L1.class);
            Root<L1> root = criteriaQuery.from(L1.class);
            criteriaQuery.where(criteriaBuilder.greaterThan(root.get("id"), 0));
            criteriaQuery.orderBy(criteriaBuilder.asc(root.get(L1_.id)));
            final TypedQuery<L1> query = em.createQuery(criteriaQuery);
            query.setFirstResult(1);
            query.setMaxResults(10);
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            final List<L1> results = query.getResultList();
            assertNotNull(results);
            assertEquals(1, results.size());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            if(em.isOpen()) {
                em.close();
            }
        }
    }

    private DatabasePlatform getPlatform(EntityManagerFactory emf) {
        return ((EntityManagerFactoryImpl)emf).getServerSession().getPlatform();
    }
}
