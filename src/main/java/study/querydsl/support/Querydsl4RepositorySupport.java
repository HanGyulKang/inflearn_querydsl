/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package study.querydsl.support;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.querydsl.core.dml.DeleteClause;
import com.querydsl.core.dml.UpdateClause;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.PathBuilderFactory;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAUpdateClause;

import java.util.List;
import java.util.function.Function;

/**
 * Base class for implementing repositories using Querydsl library.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@Repository
public abstract class Querydsl4RepositorySupport {

	private final PathBuilder<?> builder;

	private @Nullable EntityManager entityManager;
	private @Nullable Querydsl querydsl;

	/**
	 * Creates a new {@link Querydsl4RepositorySupport} instance for the given domain type.
	 *
	 * @param domainClass must not be {@literal null}.
	 */
	public Querydsl4RepositorySupport(Class<?> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");
		this.builder = new PathBuilderFactory().create(domainClass);
	}

	/**
	 * Setter to inject {@link EntityManager}.
	 *
	 * @param entityManager must not be {@literal null}.
	 */
	@Autowired
	public void setEntityManager(EntityManager entityManager) {

		Assert.notNull(entityManager, "EntityManager must not be null!");
		this.querydsl = new Querydsl(entityManager, builder);
		this.entityManager = entityManager;
	}

	/**
	 * Callback to verify configuration. Used by containers.
	 */
	@PostConstruct
	public void validate() {
		Assert.notNull(entityManager, "EntityManager must not be null!");
		Assert.notNull(querydsl, "Querydsl must not be null!");
	}

	/**
	 * Returns the {@link EntityManager}.
	 *
	 * @return the entityManager
	 */
	@Nullable
	protected EntityManager getEntityManager() {
		return entityManager;
	}

	@Nullable
	protected JPAQueryFactory getQueryFactory() {
		return new JPAQueryFactory(this::getEntityManager);
	}
	/**
	 * Returns a fresh {@link JPQLQuery}.
	 *
	 * @param paths must not be {@literal null}.
	 * @return the Querydsl {@link JPQLQuery}.
	 */
	protected JPQLQuery<Object> from(EntityPath<?>... paths) {
		return getRequiredQuerydsl().createQuery(paths);
	}

	/**
	 * Returns a {@link JPQLQuery} for the given {@link EntityPath}.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	protected <T> JPQLQuery<T> from(EntityPath<T> path) {
		return getRequiredQuerydsl().createQuery(path).select(path);
	}

	/**
	 * Returns a fresh {@link DeleteClause}.
	 *
	 * @param path
	 * @return the Querydsl {@link DeleteClause}.
	 */
	protected DeleteClause<JPADeleteClause> delete(EntityPath<?> path) {
		return new JPADeleteClause(getRequiredEntityManager(), path);
	}

	/**
	 * Returns a fresh {@link UpdateClause}.
	 *
	 * @param path
	 * @return the Querydsl {@link UpdateClause}.
	 */
	protected UpdateClause<JPAUpdateClause> update(EntityPath<?> path) {
		return new JPAUpdateClause(getRequiredEntityManager(), path);
	}

	/**
	 * Returns a {@link PathBuilder} for the configured domain type.
	 *
	 * @param <T>
	 * @return the Querdsl {@link PathBuilder}.
	 */
	@SuppressWarnings("unchecked")
	protected <T> PathBuilder<T> getBuilder() {
		return (PathBuilder<T>) builder;
	}

	/**
	 * Returns the underlying Querydsl helper instance.
	 *
	 * @return
	 */
	@Nullable
	protected Querydsl getQuerydsl() {
		return this.querydsl;
	}

	private Querydsl getRequiredQuerydsl() {

		if (querydsl == null) {
			throw new IllegalStateException("Querydsl is null!");
		}

		return querydsl;
	}

	private EntityManager getRequiredEntityManager() {

		if (entityManager == null) {
			throw new IllegalStateException("EntityManager is null!");
		}

		return entityManager;
	}

	protected <T> Page<T> applyPagination(Pageable pageable,
										  Function<JPAQueryFactory, JPAQuery> contentQuery) {
		JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
		List<T> content = getQuerydsl().applyPagination(pageable, jpaQuery).fetch();
		return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
	}

	protected <T> Page<T> applyPagination(Pageable pageable,
										  Function<JPAQueryFactory, JPAQuery> contentQuery,
										  Function<JPAQueryFactory, JPAQuery> countQuery) {
		JPAQuery jpaContentQuery = contentQuery.apply(getQueryFactory());
		List<T> content = getQuerydsl().applyPagination(pageable, jpaContentQuery).fetch();
		JPAQuery jpaCountQuery = countQuery.apply(getQueryFactory());

		return PageableExecutionUtils.getPage(content, pageable, jpaCountQuery::fetchCount);
	}
}
