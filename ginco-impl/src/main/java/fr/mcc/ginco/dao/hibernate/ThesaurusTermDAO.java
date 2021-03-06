/**
 * Copyright or © or Copr. Ministère Français chargé de la Culture
 * et de la Communication (2013)
 * <p/>
 * contact.gincoculture_at_gouv.fr
 * <p/>
 * This software is a computer program whose purpose is to provide a thesaurus
 * management solution.
 * <p/>
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * <p/>
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 * <p/>
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systemsand/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 * <p/>
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
package fr.mcc.ginco.dao.hibernate;

import java.math.BigInteger;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import fr.mcc.ginco.beans.ThesaurusConcept;
import fr.mcc.ginco.beans.ThesaurusTerm;
import fr.mcc.ginco.dao.IThesaurusTermDAO;
import fr.mcc.ginco.enums.TermStatusEnum;
import fr.mcc.ginco.exceptions.BusinessException;

/**
 * Implementation of the data access object to the thesaurus_term database table
 */
@Repository
public class ThesaurusTermDAO extends
		GenericHibernateDAO<ThesaurusTerm, String> implements IThesaurusTermDAO {

	private static final String LEXICAL_VALUE = "lexicalValue";
	private static final String LANGUAGE_ID = "language.id";
	private static final String STATUS = "status";
	private static final String CONCEPT = "concept";
	private static final String PREFERED = "prefered";
	private static final String CONCEPT_IDENTIFIER = "concept.identifier";
	private static final String THESAURUS_IDENTIFIER = "thesaurus.identifier";

	@Value("${ginco.default.language}")
	private String defaultLang;

	public ThesaurusTermDAO() {
		super(ThesaurusTerm.class);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#findPaginatedItems(java.lang.Integer, java.lang.Integer, java.lang.String)
	 */
	@Override
	public List<ThesaurusTerm> findPaginatedSandboxedItems(Integer start, Integer limit,
	                                                       String idThesaurus) {
		Criteria criteria = getCurrentSession().createCriteria(ThesaurusTerm.class);
		getSandboxedTerms(criteria, start, limit, idThesaurus);
		return criteria.list();
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#findPaginatedSandboxedValidatedItems(java.lang.Integer, java.lang.Integer, java.lang.String)
	 */
	@Override
	public List<ThesaurusTerm> findPaginatedSandboxedValidatedItems(
			Integer startIndex, Integer limit, String idThesaurus) {
		Criteria criteria = getCurrentSession().createCriteria(ThesaurusTerm.class);
		getSandboxedTerms(criteria, startIndex, limit, idThesaurus);
		criteria.add(Restrictions.eq(STATUS, TermStatusEnum.VALIDATED.getStatus()));
		return criteria.list();
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#countSandboxedTerms(java.lang.String)
	 */
	@Override
	public Long countSandboxedTerms(String idThesaurus) {
		Criteria criteria = getCurrentSession().createCriteria(ThesaurusTerm.class);
		countAllSandboxedTerms(criteria, idThesaurus);
		return (Long) criteria.list().get(0);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#countPreferredTerms(java.lang.String)
	 */
	@Override
	public Long countPreferredTerms(String idThesaurus) {
		Criteria criteria = getCurrentSession().createCriteria(ThesaurusTerm.class);
		criteria.add(Restrictions.eq(THESAURUS_IDENTIFIER, idThesaurus))
				.add(Restrictions.isNotNull(CONCEPT))
				.add(Restrictions.eq(PREFERED, Boolean.TRUE))
				.setProjection(Projections.rowCount());
		return (Long) criteria.list().get(0);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#countNonPreferredTerms(java.lang.String)
	 */
	@Override
	public Long countNonPreferredTerms(String idThesaurus) {
		Criteria criteria = getCurrentSession().createCriteria(ThesaurusTerm.class);
		criteria.add(Restrictions.eq(THESAURUS_IDENTIFIER, idThesaurus))
				.add(Restrictions.isNotNull(CONCEPT))
				.add(Restrictions.eq(PREFERED, Boolean.FALSE))
				.setProjection(Projections.rowCount());
		return (Long) criteria.list().get(0);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#countTerms(java.lang.String)
	 */
	@Override
	public Long countTerms(String idThesaurus) {
		Criteria criteria = getCurrentSession().createCriteria(ThesaurusTerm.class);
		criteria.add(Restrictions.eq(THESAURUS_IDENTIFIER, idThesaurus))
				.setProjection(Projections.rowCount());
		return (Long) criteria.list().get(0);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#countTermsWoNotes(java.lang.String)
	 */
	@Override
	public Long countTermsWoNotes(String idThesaurus) {
		Query query = getCurrentSession().createSQLQuery(
				"select count(*) " + "from thesaurus_term t "
						+ "left join note n on t.identifier = n.termid "
						+ "where t.thesaurusid=:pthesaurusid"
						+ " and n.identifier is null"
		);
		query.setParameter("pthesaurusid", idThesaurus);
		return ((BigInteger) query.list().get(0)).longValue();
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#countSandboxedValidatedTerms(java.lang.String)
	 */
	@Override
	public Long countSandboxedValidatedTerms(String idThesaurus) {
		Criteria criteria = getCurrentSession().createCriteria(ThesaurusTerm.class);
		countAllSandboxedTerms(criteria, idThesaurus);
		criteria.add(Restrictions.eq(STATUS, TermStatusEnum.VALIDATED.getStatus()));
		return (Long) criteria.list().get(0);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#getConceptPreferredTerm(java.lang.String)
	 */
	@Override
	public ThesaurusTerm getConceptPreferredTerm(String conceptId) {
		List<ThesaurusTerm> list = getConceptPreferredTerms(conceptId);
		if (list.size() > 0) {
			for (ThesaurusTerm term : list) {
				if (term.getLanguage().getId().equals(defaultLang)) {
					return term;
				}
			}
		}

		return list.get(0);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#getConceptPreferredTerm(java.lang.String, java.lang.String)
	 */
	@Override
	public ThesaurusTerm getConceptPreferredTerm(String conceptId, String languageId) {
		List<ThesaurusTerm> list = getCurrentSession()
				.createCriteria(ThesaurusTerm.class)
				.add(Restrictions.eq(CONCEPT_IDENTIFIER, conceptId))
				.add(Restrictions.eq(PREFERED, Boolean.TRUE))
				.add(Restrictions.eq(LANGUAGE_ID, languageId))

				.list();

		if (list == null || list.size() == 0) {
			return null;
		}
		return list.get(0);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#findTermsByConceptId(java.lang.String)
	 */
	@Override
	public List<ThesaurusTerm> findTermsByConceptId(String conceptId) {
		List<ThesaurusTerm> list = getCurrentSession()
				.createCriteria(ThesaurusTerm.class)
				.add(Restrictions.eq(CONCEPT_IDENTIFIER, conceptId))
				.list();
		if (list.size() == 0) {
			throw new BusinessException("No term found for this concept id ! " + conceptId
					+ "Please check your database !", "invalid-term-id");
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#findTermsByThesaurusId(java.lang.String)
	 */
	@Override
	public List<ThesaurusTerm> findTermsByThesaurusId(String thesaurusId) {
		List<ThesaurusTerm> list = getCurrentSession()
				.createCriteria(ThesaurusTerm.class)
				.add(Restrictions.eq(THESAURUS_IDENTIFIER, thesaurusId))
				.list();
		return list;
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#countSimilarTermsByLexicalValueAndLanguage(fr.mcc.ginco.beans.ThesaurusTerm)
	 */
	@Override
	public Long countSimilarTermsByLexicalValueAndLanguage(ThesaurusTerm term) {
		return (Long) getCurrentSession()
				.createCriteria(ThesaurusTerm.class)
				.add(Restrictions.eq(LEXICAL_VALUE, term.getLexicalValue()))
				.add(Restrictions.eq(LANGUAGE_ID, term.getLanguage().getId()))
				.add(Restrictions.eq(THESAURUS_IDENTIFIER, term.getThesaurus().getIdentifier()))
				.add(Restrictions.ne("identifier", term.getIdentifier()))
				.setProjection(Projections.rowCount())
				.list().get(0);
	}

	@Override
	public ThesaurusTerm update(ThesaurusTerm entity) {
		return update(entity, true);
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.hibernate.GenericHibernateDAO#update(java.lang.Object)
	 */
	@Override
	public ThesaurusTerm update(ThesaurusTerm termToUpdate, boolean checkExisting) {
		getCurrentSession().setFlushMode(FlushMode.COMMIT);
		// Verifying if there is no a similar term (lexicalValue + lang)
		if (checkExisting) {
			Long numberOfExistingTerms = countSimilarTermsByLexicalValueAndLanguage(termToUpdate);
			if (numberOfExistingTerms > 0) {
				throw new BusinessException("Already existing term : "
						+ termToUpdate.getLexicalValue(), "already-existing-term",
						new Object[]{termToUpdate.getLexicalValue()}
				);
			}
		}
		if (termToUpdate.getHidden() == null) {
			//By default, hidden is false if not set
			termToUpdate.setHidden(false);
		} else if (termToUpdate.getHidden()) {
			if (termToUpdate.getPrefered()) {
				throw new BusinessException("Only non prefered terms can be hidden",
						"only-non-prefered-term-can-be-hidden");
			}
		}

		// Update an existing term
		getCurrentSession().saveOrUpdate(termToUpdate);
		//getCurrentSession().flush();
		return termToUpdate;
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#getTermByLexicalValueThesaurusIdLanguageId(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public ThesaurusTerm getTermByLexicalValueThesaurusIdLanguageId(String lexicalValue, String thesaurusId, String languageId) {
		return (ThesaurusTerm) getCurrentSession().createCriteria(ThesaurusTerm.class)
				.add(Restrictions.or(Restrictions.eq(LEXICAL_VALUE, lexicalValue.replace("'", "&apos;")),Restrictions.eq(LEXICAL_VALUE, lexicalValue)))
				.add(Restrictions.eq(THESAURUS_IDENTIFIER, thesaurusId))
				.add(Restrictions.eq(LANGUAGE_ID, languageId)).uniqueResult();
	}

	/**
	 * This method constructs a criteria to get sandboxed terms
	 *
	 * @param criteria
	 * @param startIndex
	 * @param limit
	 * @param idThesaurus
	 */
	private void getSandboxedTerms(Criteria criteria, Integer startIndex, Integer limit, String idThesaurus) {
		criteria.setMaxResults(limit)
				.add(Restrictions.eq(THESAURUS_IDENTIFIER, idThesaurus))
				.add(Restrictions.isNull(CONCEPT))
				.setFirstResult(startIndex).addOrder(Order.asc(LEXICAL_VALUE));
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#findPaginatedPreferredItems(java.lang.Integer, java.lang.Integer, java.lang.String)
	 */
	@Override
	public List<ThesaurusTerm> findPaginatedPreferredItems(Integer startIndex,
	                                                       Integer limit, String idThesaurus, Boolean onlyNotOrphanConcepts) {
		
		Criteria criteria = getCurrentSession().createCriteria(ThesaurusTerm.class,"tt");
		if (onlyNotOrphanConcepts!=null && onlyNotOrphanConcepts==true)
		{
			criteria = criteria.createAlias("concept","tc")
					.add(Restrictions.or(Restrictions.and(Restrictions.isNotNull("tc.parentConcepts"),
					Restrictions.isNotEmpty("tc.parentConcepts")),
					Restrictions.eq("tc.topConcept",  Boolean.TRUE)));
		}
		criteria.add(Restrictions.eq("tt.prefered", Boolean.TRUE));
		criteria.add(Restrictions.eq(THESAURUS_IDENTIFIER, idThesaurus))
				
				.add(Restrictions.isNotNull(CONCEPT))
				.setFirstResult(startIndex).addOrder(Order.asc(LEXICAL_VALUE));
		criteria = criteria.setMaxResults(limit);
		return criteria.list();
	}

	/**
	 * This method constructs a criteria to count sandboxed terms
	 * @param criteria
	 * @param idThesaurus
	 */
	/**
	 * @param criteria
	 * @param idThesaurus
	 */
	private void countAllSandboxedTerms(Criteria criteria, String idThesaurus) {
		criteria.add(Restrictions.eq(THESAURUS_IDENTIFIER, idThesaurus))
				.add(Restrictions.isNull(CONCEPT))
				.setProjection(Projections.rowCount());
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#getConceptNotPreferredTerms(java.lang.String)
	 */
	@Override
	public List<ThesaurusTerm> getConceptNotPreferredTerms(String conceptId) {
		List<ThesaurusTerm> list = getCurrentSession()
				.createCriteria(ThesaurusTerm.class)
				.add(Restrictions.eq(CONCEPT_IDENTIFIER, conceptId))
				.add(Restrictions.eq(PREFERED, Boolean.FALSE))
				.list();
		return list;
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#getConceptPreferredTerms(java.lang.String)
	 */
	@Override
	public List<ThesaurusTerm> getConceptPreferredTerms(String conceptId) {
		List<ThesaurusTerm> list = getCurrentSession()
				.createCriteria(ThesaurusTerm.class)
				.add(Restrictions.eq(CONCEPT_IDENTIFIER, conceptId))
				.add(Restrictions.eq(PREFERED, Boolean.TRUE))
				.list();

		if (list.size() == 0) {
			throw new BusinessException("No preferred term found ! for concept : "+conceptId
					+ " Please check your database !", "no-preferred-term-found");
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see fr.mcc.ginco.dao.IThesaurusTermDAO#getTermsWoNotes(java.lang.String, int, int)
	 */
	@Override
	public List<ThesaurusTerm> getTermsWoNotes(String idThesaurus,
	                                           int startIndex, int limit) {
		Query query = getCurrentSession().createSQLQuery("select t.* "
				+ "from thesaurus_term t "
				+ "left join note n on t.identifier = n.termid "
				+ "where t.thesaurusid=:pthesaurusid"
				+ " and n.identifier is null").addEntity(ThesaurusTerm.class);
		query.setParameter("pthesaurusid", idThesaurus);
		query.setFirstResult(startIndex);
		query.setFetchSize(limit);
		query.setMaxResults(limit);
		return (List<ThesaurusTerm>) query.list();
	}
}