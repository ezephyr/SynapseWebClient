package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.sagebionetworks.repo.web.NotFoundException;




/**
 * This class contains helper methods for DAOs. Since each DAO may need to pick
 * and choose methods from various helpers, the chosen design pattern for the
 * DAOs was that of a wrapper or adapter, rather than of a base class with
 * extensions.
 * 
 * This class is parameterized by an (implementation independent) DTO type and a
 * JDO specific JDO type. It's the DAO's job to translate between these types as
 * it persists and retrieves data.
 * 
 * @author bhoff
 * 
 * @param <S>
 *            the DTO class
 * @param <T>
 *            the JDO class
 */
abstract public class JDOBaseDAOImpl<S extends Base, T extends JDOBase>
		implements BaseDAO<S> {
	
	private static final Logger log = Logger
	.getLogger(JDOBaseDAOImpl.class.getName());


	protected String userId; // the id of the user performing the DAO
								// operations;

	public JDOBaseDAOImpl(String userId) {
		this.userId = userId;
	}

	/**
	 * Create a new instance of the data transfer object. Introducing this
	 * abstract method helps us avoid making assumptions about constructors.
	 * 
	 * @return the new object
	 */
	abstract protected S newDTO();
	
	/**
	 * @return the type of the object which the DAO serves
	 */
	public String getType() {return newDTO().getClass().getName();}

	/**
	 * Create a new instance of the persistable object. Introducing this
	 * abstract method helps us avoid making assumptions about constructors.
	 * 
	 * @return the new object
	 */
	abstract protected T newJDO();

	/**
	 * Do a shallow copy from the JDO object to the DTO object.
	 * 
	 * @param jdo
	 * @param dto
	 * @throws DatastoreException
	 */
	abstract protected void copyToDto(T jdo, S dto) throws DatastoreException;

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	abstract protected void copyFromDto(S dto, T jdo)
			throws InvalidModelException;

	/**
	 * @param jdoClass
	 *            the class parameterized by T
	 */
	abstract protected Class<T> getJdoClass();

	/**
	 * Create a clone of the given object in memory (no datastore operations)
	 * Extensions of this class can go as deep as needed in copying data to
	 * create a clone
	 * 
	 * @param jdo
	 *            the object to clone
	 * @return the clone
	 * @throws DatastoreException
	 */
	protected T cloneJdo(T jdo) throws DatastoreException {
		S dto = newDTO();

		// TODO this assumes that all DTOs reflect the contents of the JDO (a
		// one-to-one mapping), since this may not always be the case it would
		// be better to implement jdo.clone()
		copyToDto(jdo, dto);
		T clone = newJDO();
		try {
			copyFromDto(dto, clone);
		} catch (InvalidModelException ime) {
			// better not, the content just came from a jdo!
			throw new IllegalStateException(ime);
		}

		return clone;
	}

	// /**
	// * take care of any work that has to be done before deleting the
	// persistent
	// * object but within the same transaction (for example, deleteing objects
	// * which this object composes, but which are not represented by owned
	// * relationships)
	// *
	// * @param pm
	// * @param jdo
	// * the object to be deleted
	// */
	// protected void preDelete(PersistenceManager pm, T jdo) {
	// // for the base DAO, nothing needs to be done
	// }

	// /**
	// * This may be overridden by subclasses to generate the
	// * object's key. Returning null causes the system to
	// * generate the key itself.
	// * @return the key for a new object, or null if none
	// */
	// protected Long generateKey(PersistenceManager pm) throws
	// DatastoreException {
	// return null;
	// }

	// /**
	// * take care of any work that has to be done after creating the persistent
	// * object but within the same transaction
	// *
	// * @param pm
	// * @param jdo
	// */
	// protected void postCreate(PersistenceManager pm, T jdo) {
	// // for the base DAO, nothing needs to be done
	// }

	protected T createIntern(S dto) throws InvalidModelException,
			DatastoreException {
		//
		// Set system-controlled immutable fields
		//
		// Question: is this where we want to be setting immutable
		// system-controlled fields for our
		// objects? This should only be set at creation time so its not
		// appropriate to put it in copyFromDTO.
		dto.setCreationDate(new Date()); // now


		T jdo = newJDO();
		copyFromDto(dto, jdo);
		return jdo;
	}

	/**
	 * Add access to the given (newly created) object. If the user==null then
	 * make the object publicly accessible.
	 * 
	 * @param pm
	 * @param jdo
	 */
	protected void addUserAccess(PersistenceManager pm, T jdo) {
		JDOUserGroupDAOImpl groupDAO = new JDOUserGroupDAOImpl(userId);
		JDOUserGroup group = null;

		if (userId == null || userId.equals(AuthUtilConstants.ANONYMOUS_USER_ID)) {
			group = JDOUserGroupDAOImpl.getPublicGroup(pm);
		} else {
			group = groupDAO.getOrCreateIndividualGroup(pm);
		}
		// System.out.println("addUserAccess: Group is "+group.getName());
		// now add the object to the group
		Transaction tx = pm.currentTransaction();
		try {
			tx.begin();
			JDOUserGroupDAOImpl.addResourceToGroup(group, jdo.getClass().getName(), jdo.getId(), Arrays
					.asList(new  AuthorizationConstants.ACCESS_TYPE[] { AuthorizationConstants.ACCESS_TYPE.READ,
							AuthorizationConstants.ACCESS_TYPE.CHANGE,
							AuthorizationConstants.ACCESS_TYPE.SHARE }));
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}
	}

	/**
	 * Create a new object, using the information in the passed DTO
	 * 
	 * @param dto
	 * @return the ID of the created object
	 * @throws InvalidModelException
	 */
	public String create(S dto) throws InvalidModelException,
			DatastoreException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		// Note: we are migrating away from this DAO implementation.
		// I'm commenting out this section to allow the existing service tests to pass
//		try {
//		if (!(new JDOUserGroupDAOImpl(null)).canCreate(userId, getJdoClass(), pm))
//			throw new UnauthorizedException(
//					"Cannot create objects of this type.");
//		} catch (NotFoundException nfe) {
//			throw new DatastoreException();
//		}
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			T jdo = createIntern(dto);
			pm.makePersistent(jdo);
			tx.commit();
//			tx = pm.currentTransaction();
			addUserAccess(pm, jdo); 
			copyToDto(jdo, dto);
			dto.setId(KeyFactory.keyToString(jdo.getId())); // TODO Consider
															// putting this line
															// in 'copyToDto'
			return KeyFactory.keyToString(jdo.getId());
		} catch (InvalidModelException ime) {
			throw ime;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	/**
	 * 
	 * @param id
	 *            id of the object to be retrieved
	 * @return the DTO version of the retrieved object
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public S get(String id) throws DatastoreException, NotFoundException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Long key = KeyFactory.stringToKey(id);
		try {
			T jdo = (T) pm.getObjectById(getJdoClass(), key);
			//  authorization check comes AFTER the retrieval step, so that we get a 'not found' result
			// rather than 'forbidden' when an object does not exist.
			if (!JDOUserGroupDAOImpl.canAccess(userId, getJdoClass().getName(), key, AuthorizationConstants.ACCESS_TYPE.READ, pm))
				throw new UnauthorizedException();
			S dto = newDTO();
			copyToDto(jdo, dto);
			return dto;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	// // sometimes we need to delete from within another transaction
	// public void delete(PersistenceManager pm, T jdo) {
	// preDelete(pm, jdo);
	// pm.deletePersistent(jdo);
	// }

	/**
	 * Delete the specified object
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException,
			UnauthorizedException {
		PersistenceManager pm = PMF.get();
		Long key = KeyFactory.stringToKey(id);
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			T jdo = (T) pm.getObjectById(getJdoClass(), key);
			JDOUserGroupDAOImpl groupDAO = new JDOUserGroupDAOImpl(null);
			if (!groupDAO.canAccess(userId, getJdoClass().getName(), key, AuthorizationConstants.ACCESS_TYPE.CHANGE, pm))
				throw new UnauthorizedException();
			groupDAO.removeResourceFromAllGroups(jdo);
			// delete(pm, jdo);
			pm.deletePersistent(jdo);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (UnauthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	/**
	 * This updates the 'shallow' properties. Version doesn't change.
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 *             if version in dto doesn't match version of object
	 * @throws InvalidModelException
	 */
	public void update(PersistenceManager pm, S dto) throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		if (dto.getId() == null)
			throw new InvalidModelException("id is null");
		Long id = KeyFactory.stringToKey(dto.getId());
		T jdo = (T) pm.getObjectById(getJdoClass(), id);
		copyFromDto(dto, jdo);
		pm.makePersistent(jdo);
	}

	public void update(S dto) throws DatastoreException, InvalidModelException,
			NotFoundException, UnauthorizedException {
		PersistenceManager pm = PMF.get();
		// *** NOTE, if you try to do this within the transaction, below, it
		// breaks!!
		Long id = KeyFactory.stringToKey(dto.getId());
//		if (!hasAccessIntern(pm, getJdoClass().getName(), id, AuthorizationConstants.ACCESS_TYPE.CHANGE))
//			throw new UnauthorizedException();
		if (!JDOUserGroupDAOImpl.canAccess(userId, getJdoClass().getName(), id, AuthorizationConstants.ACCESS_TYPE.CHANGE, pm))
			throw new UnauthorizedException();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			update(pm, dto);
			tx.commit();
		} catch (InvalidModelException ime) {
			throw ime;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}

	}

	/**
	 * 
	 * returns the number of objects of a certain type
	 * 
	 */
	protected int getCount(PersistenceManager pm) throws DatastoreException, NotFoundException {
		Query query = pm.newQuery(getJdoClass());
		@SuppressWarnings("unchecked")
		Collection<T> c = (Collection<T>) query.execute();
		Collection<Long> keys = new HashSet<Long>();
		for (T elem : c)
			keys.add(elem.getId());
		if (!JDOUserGroupDAOImpl.isAdmin(userId, pm)) {
			Collection<Long> canAccess = getCanAccess(pm, getJdoClass(), 
				AuthorizationConstants.ACCESS_TYPE.READ);
			keys.retainAll(canAccess);
		}
		return keys.size();
	}

	public int getCount() throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			int count = getCount(pm);
			return count;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Retrieve all objects of the given type, 'paginated' by the given start
	 * and end
	 * 
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end'
	 */
	public List<S> getInRange(int start, int end) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Query query = pm.newQuery(getJdoClass());
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute());
			boolean isAdmin = JDOUserGroupDAOImpl.isAdmin(userId, pm);
			Collection<Long> canAccess = null;
			if (!isAdmin) {
				canAccess = getCanAccess(pm, getJdoClass(), AuthorizationConstants.ACCESS_TYPE.READ);
			}
			List<S> ans = new ArrayList<S>();
			int count = 0;
			for (T jdo : list) {
				if (isAdmin || canAccess.contains(jdo.getId())) {
					if (count >= start && count < end) {
						S dto = newDTO();
						copyToDto(jdo, dto);
						ans.add(dto);
					}
					count++;
				}
			}
			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Retrieve all objects of the given type, 'paginated' by the given start
	 * and end and sorted by the specified primary field
	 * 
	 * @param start
	 * @param end
	 * @param sortBy
	 * @param asc
	 *            if true then ascending, else descending
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end' and sorted by the given primary field
	 */
	public List<S> getInRangeSortedByPrimaryField(int start, int end,
			String sortBy, boolean asc) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Query query = pm.newQuery(getJdoClass());
			query.setOrdering(sortBy + (asc ? " ascending" : " descending"));
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute());
			boolean isAdmin = JDOUserGroupDAOImpl.isAdmin(userId, pm);
			Collection<Long> canAccess = null;
			if (!isAdmin) {
				canAccess = getCanAccess(pm, getJdoClass(), AuthorizationConstants.ACCESS_TYPE.READ);
			}
			List<S> ans = new ArrayList<S>();
			int count = 0;
			for (T jdo : list) {
				if (isAdmin || canAccess.contains(jdo.getId())) {
					if (count >= start && count < end) {
						S dto = newDTO();
						copyToDto(jdo, dto);
						ans.add(dto);
					}
					count++;
				}
			}
			return ans;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * Get the objects of the given type having the specified value in the given
	 * primary field, and 'paginated' by the given start/end limits
	 * 
	 * @param start
	 * @param end
	 * @param attribute
	 *            the name of the primary field
	 * @param value
	 * @return
	 */
	public List<S> getInRangeHavingPrimaryField(int start, int end,
			String attribute, Object value) throws DatastoreException, NotFoundException {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Query query = pm.newQuery(getJdoClass());
			// query.setRange(start, end);
			query.setFilter(attribute + "==pValue");
			query.declareParameters(value.getClass().getName() + " pValue");
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>) query.execute(value));
			boolean isAdmin = JDOUserGroupDAOImpl.isAdmin(userId, pm);
			Collection<Long> canAccess = null;
			if (!isAdmin) {
				canAccess = getCanAccess(pm, getJdoClass(), AuthorizationConstants.ACCESS_TYPE.READ);
			}
			List<S> ans = new ArrayList<S>();
			int count = 0;
			for (T jdo : list) {
				if (isAdmin || canAccess.contains(jdo.getId())) {
					if (count >= start && count < end) {
						S dto = newDTO();
						copyToDto(jdo, dto);
						ans.add(dto);
					}
					count++;
				}
			}
			return ans;
		} catch (NotFoundException nfe) {
			throw nfe;
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * 
	 * @return the user credentials under which the DAO operations are being
	 *         performed, or 'null' if anonymous
	 */
	// protected JDOUser getUser(PersistenceManager pm) {
	// if (this.userId==null) return null;
	// Query query = pm.newQuery
	// return (JDOUser) pm.getObjectById(JDOUser.class,
	// KeyFactory.stringToKey(userId));
	// }

	public void dumpAllAccess() throws Exception {
		PersistenceManager pm = PMF.get();
		Query query = pm.newQuery(JDOResourceAccess.class);
		@SuppressWarnings("unchecked")
		Collection<JDOResourceAccess> ras = (Collection<JDOResourceAccess>) query
		.execute();
		for (JDOResourceAccess ra: ras) {
			System.out.println("Owner: "+ra.getOwner()+" Resource: "+ra.getResourceType()+"-"+ra.getResourceId()+" type: "+ra.getAccessType());
		}
	}

	public Collection<UserGroup> whoHasAccess(S resource, AuthorizationConstants.ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException {
		// search for all JDOResourceAccess objects having the given object
		// and access type
		// return a collection of the user groups
		try {
			JDOUserGroupDAOImpl groupDAO = new JDOUserGroupDAOImpl(userId);
			Collection<UserGroup> ans = groupDAO.getAccessGroups(resource, accessType);
			return ans;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}

// THIS REPLICATES LOGIC IN JDOUserGroupDAOImpl, which belongs THERE
//	protected boolean hasAccessIntern(PersistenceManager pm, String resourceType, Long resourceKey,
//			String accessType) {
//		JDOUser thisUser = (new JDOUserDAOImpl(userId)).getUser(pm);
//		Collection<JDOResourceAccess> ras = getAccess(pm, resourceType, resourceKey,
//				accessType);
//		// System.out.println("JDOBaseDAOImpl.hasAccessIntern: ras.size()=="+ras.size());
//		for (JDOResourceAccess ra : ras) {
//			JDOUserGroup group = ra.getOwner();
//			// System.out.println("JDOBaseDAOImpl.hasAccessIntern: \tGroup "+group.getName()+" has "+group.getUsers().size()+" users.");
//			// for (Long u : group.getUsers()) System.out.print(u+", ");
//			// System.out.println();
//			if (JDOUserGroupDAOImpl.isPublicGroup(group)
//					|| (thisUser != null && group.getUsers().contains(
//							thisUser.getId())))
//				return true;
//		}
//		return false;
//	}

	public boolean hasAccess(S resource, AuthorizationConstants.ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			Long resourceKey = KeyFactory.stringToKey(resource.getId());
			return JDOUserGroupDAOImpl.canAccess(userId, getJdoClass().getName(), resourceKey, accessType, pm);
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * TODO this used to be implemented via Google Appengine Datastore JDO.  That means
	 * that joins of any complexity had to be done in
	 * memory. We can now update this code to push that logic back into the relational database.
	 * 
	 * @return all objects of the given class in the system that the user can access with the given
	 *         accesstype
	 */
	private Collection<Long> getCanAccess(PersistenceManager pm, Class<T> jdoClass, AuthorizationConstants.ACCESS_TYPE accessType) throws NotFoundException{
		// find all the groups the user is a member of
		Collection<JDOUserGroup> groups = new HashSet<JDOUserGroup>();
		if (userId != null && !AuthUtilConstants.ANONYMOUS_USER_ID.equals(userId)) {
			JDOUser user = (new JDOUserDAOImpl(userId)).getUser(pm);
			if (user==null) throw new NotFoundException(userId+" does not exist");
			Query query = pm.newQuery(JDOUserGroup.class);
			query.setFilter("users.contains(pUser)");
			query.declareParameters(Long.class.getName() + " pUser");
			@SuppressWarnings("unchecked")
			Collection<JDOUserGroup> c = (Collection<JDOUserGroup>) query
					.execute(user.getId());
			groups.addAll(c);
		}
		// add in Public group
		groups.add(JDOUserGroupDAOImpl.getPublicGroup(pm));
		// get all objects that these groups can access

		Query query = pm.newQuery(JDOResourceAccess.class);
		query.setFilter("owner==pUserGroup && resourceType==pResourceType");
		query.declareParameters(JDOUserGroup.class.getName()+ " pUserGroup, " + 
				String.class.getName() + " pResourceType");
		Collection<Long> ans = new HashSet<Long>();
		for (JDOUserGroup ug : groups) {
			@SuppressWarnings("unchecked")
			Collection<JDOResourceAccess> ras = (Collection<JDOResourceAccess>) query
					.execute(ug, jdoClass.getName());
			for (JDOResourceAccess ra : ras)
				if (ra.getAccessType().contains(accessType.name())) {
					ans.add(ra.getResourceId());
				}
		}
		return ans;
	}

}