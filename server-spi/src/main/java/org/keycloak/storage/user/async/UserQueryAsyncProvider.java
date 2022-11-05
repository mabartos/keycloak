package org.keycloak.storage.user.async;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.smallrye.mutiny.Uni.createFrom;

public interface UserQueryAsyncProvider {

    /**
     * Returns the number of users, without consider any service account.
     *
     * @param realm the realm
     * @return the number of users
     */
    default Uni<Integer> getUsersCount(RealmModel realm) {
        return getUsersCount(realm, false);
    }

    /**
     * Returns the number of users that are in at least one of the groups
     * given.
     *
     * @param realm    the realm
     * @param groupIds set of groups IDs, the returned user needs to belong to at least one of them
     * @return the number of users that are in at least one of the groups
     */
    default Uni<Integer> getUsersCount(RealmModel realm, Set<String> groupIds) {
        if (CollectionUtil.isEmpty(groupIds)) {
            return createFrom().item(0);
        }
        return countUsersInGroups(searchForUserStream(realm, Collections.emptyMap()), groupIds);
    }

    /**
     * Returns the number of users that would be returned by a call to {@link #searchForUserStream(RealmModel, String) searchForUserStream}
     *
     * @param realm  the realm
     * @param search case insensitive list of strings separated by whitespaces.
     * @return number of users that match the search
     */
    default Uni<Integer> getUsersCount(RealmModel realm, String search) {
        return searchForUserStream(realm, search).collect()
                .with(Collectors.counting())
                .onItem()
                .transform(Long::intValue);
    }

    /**
     * Returns the number of users that would be returned by a call to {@link #searchForUserStream(RealmModel, String) searchForUserStream}
     * and are members of at least one of the groups given by the {@code groupIds} set.
     *
     * @param realm    the realm
     * @param search   case insensitive list of strings separated by whitespaces.
     * @param groupIds set of groups IDs, the returned user needs to belong to at least one of them
     * @return number of users that match the search and given groups
     */
    default Uni<Integer> getUsersCount(RealmModel realm, String search, Set<String> groupIds) {
        if (CollectionUtil.isEmpty(groupIds)) {
            return createFrom().item(0);
        }
        return countUsersInGroups(searchForUserStream(realm, search), groupIds);
    }

    /**
     * Returns the number of users that match the given filter parameters.
     *
     * @param realm  the realm
     * @param params filter parameters
     * @return number of users that match the given filters
     */
    default Uni<Integer> getUsersCount(RealmModel realm, Map<String, String> params) {
        return searchForUserStream(realm, params).collect()
                .with(Collectors.counting())
                .onItem()
                .transform(Long::intValue);
    }

    /**
     * Returns the number of users that match the given filter parameters and is in
     * at least one of the given groups.
     *
     * @param params   filter parameters
     * @param realm    the realm
     * @param groupIds set if groups to check for
     * @return number of users that match the given filters and groups
     */
    default Uni<Integer> getUsersCount(RealmModel realm, Map<String, String> params, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return createFrom().item(0);
        }
        return countUsersInGroups(searchForUserStream(realm, params), groupIds);
    }


    /**
     * Returns the number of users from the given list of users that are in at
     * least one of the groups given in the groups set.
     *
     * @param users    list of users to check
     * @param groupIds id of groups that should be checked for
     * @return number of users that are in at least one of the groups
     */
    static Uni<Integer> countUsersInGroups(Multi<UserModel> users, Set<String> groupIds) {
        return users.filter(u -> u.getGroupsStream().map(GroupModel::getId).anyMatch(groupIds::contains))
                .collect()
                .with(Collectors.counting())
                .onItem()
                .transform(Long::intValue);
    }

    /**
     * Returns the number of users.
     *
     * @param realm                 the realm
     * @param includeServiceAccount if true, the number of users will also include service accounts. Otherwise, only the number of users.
     * @return the number of users
     */
    default Uni<Integer> getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Searches all users in the realm.
     *
     * @param realm a reference to the realm.
     * @return a non-null {@link Stream} of users.
     * @deprecated Use {@link #searchForUserStream(RealmModel, Map)} with an empty params map instead.
     */
    @Deprecated
    default Multi<UserModel> getUsersStream(RealmModel realm) {
        return searchForUserStream(realm, Collections.emptyMap());
    }

    /**
     * Searches all users in the realm, starting from the {@code firstResult} and containing at most {@code maxResults}.
     *
     * @param realm       a reference to the realm.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users.
     * @deprecated Use {@link #searchForUserStream(RealmModel, Map, Integer, Integer)} with an empty params map instead.
     */
    @Deprecated
    default Multi<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return searchForUserStream(realm, Collections.emptyMap(), firstResult, maxResults);
    }

    /**
     * Searches for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace.
     * <p/>
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * This method is used by the admin console search box
     *
     * @param realm  a reference to the realm.
     * @param search case insensitive list of string separated by whitespaces.
     * @return a non-null {@link Stream} of users that match the search string.
     */
    default Multi<UserModel> searchForUserStream(RealmModel realm, String search) {
        return searchForUserStream(realm, search, null, null);
    }

    /**
     * Searches for users whose username, email, first name or last name contain any of the strings in {@code search} separated by whitespace.
     * <p/>
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * This method is used by the admin console search box
     *
     * @param realm       a reference to the realm.
     * @param search      case insensitive list of string separated by whitespaces.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    Multi<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults);

    /**
     * Searches for user by parameter.
     * If possible, implementations should treat the parameter values as partial match patterns (i.e. in RDMBS terms use LIKE).
     * <p/>
     * Valid parameters are:
     * <ul>
     *     <li>{@link UserModel#FIRST_NAME} - first name (case insensitive string)</li>
     *     <li>{@link UserModel#LAST_NAME} - last name (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL} - email (case insensitive string)</li>
     *     <li>{@link UserModel#USERNAME} - username (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL_VERIFIED} - search only for users with verified/non-verified email (true/false)</li>
     *     <li>{@link UserModel#ENABLED} - search only for enabled/disabled users (true/false)</li>
     *     <li>{@link UserModel#IDP_ALIAS} - search only for users that have a federated identity
     *     from idp with the given alias configured (case sensitive string)</li>
     *     <li>{@link UserModel#IDP_USER_ID} - search for users with federated identity with
     *     the given userId (case sensitive string)</li>
     * </ul>
     * <p>
     * This method is used by the REST API when querying users.
     *
     * @param realm  a reference to the realm.
     * @param params a map containing the search parameters.
     * @return a non-null {@link Stream} of users that match the search parameters.
     */
    default Multi<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params) {
        return searchForUserStream(realm, params, null, null);
    }

    /**
     * Searches for user by parameter. If possible, implementations should treat the parameter values as partial match patterns
     * (i.e. in RDMBS terms use LIKE).
     * <p/>
     * Valid parameters are:
     * <ul>
     *     <li>{@link UserModel#FIRST_NAME} - first name (case insensitive string)</li>
     *     <li>{@link UserModel#LAST_NAME} - last name (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL} - email (case insensitive string)</li>
     *     <li>{@link UserModel#USERNAME} - username (case insensitive string)</li>
     *     <li>{@link UserModel#EMAIL_VERIFIED} - search only for users with verified/non-verified email (true/false)</li>
     *     <li>{@link UserModel#ENABLED} - search only for enabled/disabled users (true/false)</li>
     *     <li>{@link UserModel#IDP_ALIAS} - search only for users that have a federated identity
     *     from idp with the given alias configured (case sensitive string)</li>
     *     <li>{@link UserModel#IDP_USER_ID} - search for users with federated identity with
     *     the given userId (case sensitive string)</li>
     * </ul>
     * <p>
     * Any other parameters will be treated as custom user attributes.
     * <p>
     * This method is used by the REST API when querying users.
     *
     * @param realm       a reference to the realm.
     * @param params      a map containing the search parameters.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    Multi<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults);

    /**
     * Obtains users that belong to a specific group.
     *
     * @param realm a reference to the realm.
     * @param group a reference to the group.
     * @return a non-null {@link Stream} of users that belong to the group.
     */
    default Multi<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        return getGroupMembersStream(realm, group, null, null);
    }

    /**
     * Obtains users that belong to a specific group.
     *
     * @param realm       a reference to the realm.
     * @param group       a reference to the group.
     * @param firstResult first result to return. Ignored if negative, zero, or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that belong to the group.
     */
    Multi<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults);

    /**
     * Obtains users that have the specified role.
     *
     * @param realm a reference to the realm.
     * @param role  a reference to the role.
     * @return a non-null {@link Stream} of users that have the specified role.
     */
    default Multi<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role) {
        return getRoleMembersStream(realm, role, null, null);
    }

    /**
     * Searches for users that have the specified role.
     *
     * @param realm       a reference to the realm.
     * @param role        a reference to the role.
     * @param firstResult first result to return. Ignored if negative or {@code null}.
     * @param maxResults  maximum number of results to return. Ignored if negative or {@code null}.
     * @return a non-null {@link Stream} of users that have the specified role.
     */
    default Multi<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        return Multi.createFrom().empty();
    }

    /**
     * Searches for users that have a specific attribute with a specific value.
     *
     * @param realm     a reference to the realm.
     * @param attrName  the attribute name.
     * @param attrValue the attribute value.
     * @return a non-null {@link Stream} of users that match the search criteria.
     */
    Multi<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue);

}
