package net.nnwsf.persistence.executor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.reactive.mutiny.Mutiny.Query;
import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.smallrye.mutiny.Uni;
import net.nnwsf.query.KeyValueTerm;
import net.nnwsf.query.OperatorTerm;
import net.nnwsf.query.SearchTerm;
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;
import net.nnwsf.util.Pair;
import net.nnwsf.util.ReflectionHelper;

public class FindWithPageRequestExecutor extends Executor {

    private final String baseQuery;
    private final String baseCountQuery;
    private final String orderClause;

    private final Map<String, Field> fields;

    public FindWithPageRequestExecutor(Class<?> entityClass, Class<?> idClass, String idColumn, Method method) {
        super(entityClass, idClass, method);
        baseQuery = "select e from " + entityClass.getSimpleName() + " e";
        baseCountQuery = "select Count(e) from " + entityClass.getSimpleName() + " e";
        orderClause = " order by " + idColumn;
        fields = ReflectionHelper.findFields(entityClass);
    }

    @Override
    public Uni<?> execute(Session session, Object[] params) {
        PageRequest aPageRequest = null;
        if(PageRequest.class.isInstance(params[0])) {
            aPageRequest = (PageRequest)params[0];
        } else if( params[0] == null) {
            aPageRequest = PageRequest.of(0, 100);
        }
        if(aPageRequest != null) {
            Query<Long> countQuery;
            Query<Collection<?>> query;
            PageRequest pageRequest = aPageRequest;
            int start = pageRequest.getPage() * pageRequest.getSize();
            SearchTerm searchTerm = (SearchTerm)params[1];
            if(searchTerm != null) {
                StringBuilder whereClauseBuilder = new StringBuilder();
                List<Pair<String, Object>> parameters = new ArrayList<>();
                whereClauseBuilder.append(" where");
                appendSearchTerm(searchTerm, whereClauseBuilder, parameters);

                countQuery = session.createQuery(baseCountQuery + whereClauseBuilder.toString());
                query = session.createQuery(baseQuery + whereClauseBuilder.toString() + orderClause);
                parameters.forEach(aParameter -> {
                    countQuery.setParameter(aParameter.getFirst(), aParameter.getSecond());
                    query.setParameter(aParameter.getFirst(), aParameter.getSecond());
                });
                
            } else {
                countQuery = session.createQuery(baseCountQuery);
                query = session.createQuery(baseQuery + orderClause);

            }
            query.setFirstResult(start);
            query.setMaxResults(pageRequest.getSize());
            return countQuery.getSingleResult().chain(count -> {
                return query.getResultList()
                    .map(list -> new Page<>(count, pageRequest, list));
                });
        } else {
            throw new IllegalArgumentException("Unable to execute db query because of invalid parameters: " + Arrays.stream(params).map(Object::toString).collect(Collectors.joining(",")));
        }
    }

    private void appendSearchTerm(SearchTerm aSearchTerm, StringBuilder aStringBuilder, List<Pair<String,Object>> parameters) {
        int count = parameters.size() + 1;
        if(aSearchTerm instanceof KeyValueTerm) {
            KeyValueTerm aKeyValueTerm = (KeyValueTerm)aSearchTerm;
            try {
                Field aField = fields.get(aKeyValueTerm.getKey());
                aStringBuilder.append(" ");
                aStringBuilder.append(aKeyValueTerm.getKey());
                aStringBuilder.append(" = :p");
                aStringBuilder.append(count);
                Object value;
                if(aField.getType().isAssignableFrom(String.class)) {
                    value = "'" + aKeyValueTerm.getValue() + "'";
                } else if(aField.getType().isAssignableFrom(int.class) || aField.getType().isAssignableFrom(Integer.class)) {
                    value = Integer.parseInt(aKeyValueTerm.getValue());
                } else if(aField.getType().isAssignableFrom(short.class) || aField.getType().isAssignableFrom(Short.class)) {
                    value = Short.parseShort(aKeyValueTerm.getValue());
                } else if(aField.getType().isAssignableFrom(long.class) || aField.getType().isAssignableFrom(Long.class)) {
                    value = Long.parseLong(aKeyValueTerm.getValue());
                } else if(aField.getType().isAssignableFrom(float.class) || aField.getType().isAssignableFrom(Float.class)) {
                    value = Float.parseFloat(aKeyValueTerm.getValue());
                } else if(aField.getType().isAssignableFrom(double.class) || aField.getType().isAssignableFrom(Double.class)) {
                    value = Double.parseDouble(aKeyValueTerm.getValue());
                } else {
                    value = aKeyValueTerm.getValue();
                }
                parameters.add(Pair.of("p"+count, value));
            } catch(Exception nsf) {
                throw new RuntimeException("Invalid field " + aKeyValueTerm.getKey() + " in entity" + entityClass);
            }
        } else if(aSearchTerm instanceof OperatorTerm) {
            OperatorTerm anOperatorTerm = (OperatorTerm)aSearchTerm;
            aStringBuilder.append(" ");
            for(int i=0; i< anOperatorTerm.getValues().size(); i++) {
                if(i>0) {
                    aStringBuilder.append(" ");
                    aStringBuilder.append(anOperatorTerm.getOperator());
                }
                appendSearchTerm(anOperatorTerm.getValues().get(i), aStringBuilder, parameters);
            }
        }
    }    
}
