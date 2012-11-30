package org.springframework.social.alfresco.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.json.simple.parser.JSONParser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.social.OperationNotPermittedException;
import org.springframework.social.alfresco.api.Alfresco;
import org.springframework.social.alfresco.api.entities.Activity;
import org.springframework.social.alfresco.api.entities.AlfrescoList;
import org.springframework.social.alfresco.api.entities.Comment;
import org.springframework.social.alfresco.api.entities.Container;
import org.springframework.social.alfresco.api.entities.LegacySite;
import org.springframework.social.alfresco.api.entities.Member;
import org.springframework.social.alfresco.api.entities.Metadata;
import org.springframework.social.alfresco.api.entities.Network;
import org.springframework.social.alfresco.api.entities.Pagination;
import org.springframework.social.alfresco.api.entities.Person;
import org.springframework.social.alfresco.api.entities.Preference;
import org.springframework.social.alfresco.api.entities.Rating;
import org.springframework.social.alfresco.api.entities.Role;
import org.springframework.social.alfresco.api.entities.Site;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.springframework.social.alfresco.api.entities.Tag;
import org.springframework.social.alfresco.api.entities.UserActivationRequest;
import org.springframework.social.alfresco.api.entities.UserActivationResponse;
import org.springframework.social.alfresco.api.entities.UserRegistration;
import org.springframework.social.alfresco.api.entities.UserRegistrationRequest;
import org.springframework.social.alfresco.api.entities.UserRegistrationResponse;
import org.springframework.social.alfresco.connect.exception.AlfrescoException;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractAlfrescoTemplate implements Alfresco
{
    protected static final Log   log     = LogFactory.getLog(AlfrescoTemplate.class);

	protected ObjectMapper mapper  = new ObjectMapper();
	protected HttpHeaders  headers = new HttpHeaders();
	protected RestTemplate restTemplate;
	protected String baseUrl;
	protected JSONParser parser = new JSONParser();
	protected AuthenticationProvider authenticationProvider;
	protected boolean production;

	protected int    VERSION_NO                = 1;
	protected String VERSION                   = "/public/alfresco/versions/" + VERSION_NO + "/";

	protected PublicApiUrl ROOT_ATOMPUB_URL;
	protected PublicApiUrl ATOMPUB_URL;
	protected PublicApiUrl NETWORKS_URL;
	protected PublicApiUrl NETWORK_URL;
	protected PublicApiUrl SITES_URL;
	protected PublicApiUrl SITE_URL;
	protected PublicApiUrl CONTAINERS_URL;
	protected PublicApiUrl CONTAINER_URL;
	protected PublicApiUrl MEMBERS_URL;
    protected PublicApiUrl MEMBER_URL;
    protected PublicApiUrl PEOPLE_URL;
    protected PublicApiUrl PEOPLE_SITES_URL;
    protected PublicApiUrl PEOPLE_SITE_URL;
    protected PublicApiUrl PEOPLE_FAVORITE_SITES_URL;
    protected PublicApiUrl PEOPLE_PREFERENCES_URL;
    protected PublicApiUrl PEOPLE_PREFERENCE_URL;
    protected PublicApiUrl PEOPLE_NETWORKS_URL;
    protected PublicApiUrl PEOPLE_NETWORK_URL;
    protected PublicApiUrl PEOPLE_ACTIVITIES_URL;
    protected PublicApiUrl TAGS_URL;
    protected PublicApiUrl TAG_URL;
    protected PublicApiUrl NODE_COMMENTS_URL;
    protected PublicApiUrl NODE_COMMENT_URL;
    protected PublicApiUrl NODE_TAGS_URL;
    protected PublicApiUrl NODE_TAG_URL;
    protected PublicApiUrl NODE_RATINGS_URL;
    protected PublicApiUrl NODE_RATING_URL;
	protected PublicApiUrl CREATE_SITE_URL;
	protected PublicApiUrl REGISTER_USER_URL;
	protected PublicApiUrl ACTIVATE_USER_URL;
	protected PublicApiUrl BASE_NODE_URL;
	
	public AbstractAlfrescoTemplate(String baseUrl, boolean production)
	{
		this.baseUrl = baseUrl + "/";
		this.production = production;
		this.ROOT_ATOMPUB_URL          = new PublicApiServiceUrl(baseUrl, "cmis/versions/1.0/atom");
		this.ATOMPUB_URL               = new PublicApiNetworkUrl(baseUrl, "{network}/public/cmis/versions/1.0/atom");
		this.NETWORKS_URL              = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/networks");
		this.NETWORK_URL               = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/networks/{network}");
		this.SITES_URL                 = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/sites");
		this.SITE_URL                  = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/sites/{site}");
		this.CONTAINERS_URL            = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/sites/{site}/containers");
		this.CONTAINER_URL             = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/sites/{site}/containers/{container}");
		this.MEMBERS_URL               = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/sites/{site}/members");
		this.MEMBER_URL                = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/sites/{site}/members/{member}");
		this.PEOPLE_URL                = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}");
		this.PEOPLE_SITES_URL          = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/sites");
		this.PEOPLE_SITE_URL           = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/sites/{site}");
		this.PEOPLE_FAVORITE_SITES_URL = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/favorite-sites");
		this.PEOPLE_PREFERENCES_URL    = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/preferences");
		this.PEOPLE_PREFERENCE_URL     = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/preferences/{preference}");
		this.PEOPLE_NETWORKS_URL       = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/networks");
		this.PEOPLE_NETWORK_URL        = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/networks/{network}");
		this.PEOPLE_ACTIVITIES_URL     = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/activities");
		this.TAGS_URL                  = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/tags");
		this.TAG_URL                   = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/people/{person}/tags/{tag}");
		this.NODE_COMMENTS_URL         = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/nodes/{node}/comments");
		this.NODE_COMMENT_URL          = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/nodes/{node}/comments/{comment}");
	    this.NODE_TAGS_URL             = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/nodes/{node}/tags/");
	    this.NODE_TAG_URL              = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/nodes/{node}/tags/{tag}");
	    this.NODE_RATINGS_URL          = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/nodes/{node}/ratings");
	    this.NODE_RATING_URL 		   = new PublicApiNetworkUrl(baseUrl, "{network}/public/alfresco/versions/1/nodes/{node}/ratings/{rating}");
	    this.CREATE_SITE_URL           = new PublicApiNetworkUrl(baseUrl, "{network}/api/sites");
	    this.BASE_NODE_URL             = new PublicApiNetworkUrl(baseUrl, "{network}" + VERSION + "nodes/{node}/");
	    this.REGISTER_USER_URL     	   = new PublicApiServiceUrl(baseUrl, "internal/cloud/accounts/signupqueue");
	    this.ACTIVATE_USER_URL         = new PublicApiServiceUrl(baseUrl, "internal/cloud/account-activations");
	}

	private ThreadLocal<CMISSessions> cmisSession = new ThreadLocal<CMISSessions>() {
		@Override
		protected CMISSessions initialValue()
		{
			return new CMISSessions();
		}
	};
	
	protected abstract Map<String, String> getCMISParameters();

	protected Session createCMISSession(String networkId)
	{
		// default factory implementation
		SessionFactoryImpl sessionFactory = SessionFactoryImpl.newInstance();
		Map<String, String> parameters = new HashMap<String, String>();

		// connection settings
		parameters.put(SessionParameter.ATOMPUB_URL, ATOMPUB_URL.getUrl(networkId));
		parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
		parameters.put(SessionParameter.REPOSITORY_ID, networkId);
		
		Map<String, String> parameterOverrides = getCMISParameters();
		if(parameterOverrides != null)
		{
			parameters.putAll(parameterOverrides);
		}

		AlfrescoObjectFactoryImpl objectFactory = new AlfrescoObjectFactoryImpl();
		
		// create session
		Session session = sessionFactory.createSession(parameters, objectFactory, authenticationProvider, null);

		objectFactory.initialize(session, parameters);
		
		return session;
	}
    
	public java.util.List<Repository> getCMISNetworks()
	{
		// default factory implementation
		SessionFactoryImpl sessionFactory = SessionFactoryImpl.newInstance();
		Map<String, String> parameters = new HashMap<String, String>();

		// connection settings
		parameters.put(SessionParameter.ATOMPUB_URL, ROOT_ATOMPUB_URL.getUrl(null));
		parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

		return sessionFactory.getRepositories(parameters, null, authenticationProvider, null);
	}

	protected void configureRestTemplate()
	{
		restTemplate.setErrorHandler(new ResponseErrorHandler()
		{
			public boolean hasError(ClientHttpResponse response) throws IOException
			{
				HttpStatus status = response.getStatusCode();
				Series series = status.series();
				return series == Series.CLIENT_ERROR || series == Series.SERVER_ERROR;
			}

			private String getResponseBody(ClientHttpResponse response)
			{
				MediaType contentType = response.getHeaders().getContentType();
				Charset charset = contentType != null ? contentType.getCharSet() : null;
				
				String ret = null;
				try
				{
		            InputStream responseBody = response.getBody();
		            if (responseBody != null)
		            {
		            	byte[] b = FileCopyUtils.copyToByteArray(responseBody);
		            	ret = new String(b, charset);
		            }
				}
				catch (IOException ex)
				{
		            // ignore
				}
		        return ret;
			}

			public void handleError(ClientHttpResponse response) throws IOException
			{
				HttpStatus statusCode = response.getStatusCode();
				String body = getResponseBody(response);
				StringBuilder message = new StringBuilder(statusCode.toString());
				message.append("\n response status \n");
				message.append(response.getStatusText());
				message.append("\n response body \n");
				message.append(body);

				switch (statusCode.series()) {
					case CLIENT_ERROR:
						throw new AlfrescoException(statusCode, message.toString());
					case SERVER_ERROR:
						throw new AlfrescoException(statusCode, message.toString());
					default:
						throw new RestClientException("Unknown status code [" + statusCode + "]");
				}
			}
		});
	}
	
	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	public Session getCMISSession(String networkId) {
		CMISSessions sessions = cmisSession.get();
		Session session = sessions.getSession(networkId);
		return session;
	}

    public Network getNetwork(String networkId)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = Collections.singletonMap(TemplateParams.NETWORK, networkId);
        String response = getRestTemplate().getForObject(NETWORK_URL.getUrl(networkId), String.class, vars);
        log.debug("getNetwork: " + response);
        Response<Network> n = mapper.readValue(response, entryResponseType(Network.class));
        return n.getEntry();
    }


    public AlfrescoList<Network> getNetworks()
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getNetworks(null);
    }


    public AlfrescoList<Network> getNetworks(Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        String response = getRestTemplate().getForObject(NETWORKS_URL.getUrl(null) + generateQueryString(parameters), String.class);
        log.debug("getNetworks: " + response);
        Response<Network> n = mapper.readValue(response, entryResponseType(Network.class));
        return n.getList();
    }


    public Site getSite(String site, String networkId)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, networkId);
        vars.put(TemplateParams.SITE, site);

        String response = getRestTemplate().getForObject(SITE_URL.getUrl(networkId), String.class, vars);
        log.debug("getSite: " + response);
        Response<Site> s = mapper.readValue(response, entryResponseType(Site.class));
        return s.getEntry();

    }


    public AlfrescoList<Site> getSites(String network)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        // Use empty hashmap to avoid ambiguity in method signature with a null
        return getSites(network, new HashMap<String, String>());
    }


    public AlfrescoList<Site> getSites(String networkId, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = Collections.singletonMap(TemplateParams.NETWORK + generateQueryString(parameters), networkId);
        String response = getRestTemplate().getForObject(SITES_URL.getUrl(networkId), String.class, vars);
        log.debug("getSites: " + response);
        Response<Site> s = mapper.readValue(response, entryResponseType(Site.class));
        return s.getList();
    }


    public Container getContainer(String networkId, String site, String contatiner)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, networkId);
        vars.put(TemplateParams.SITE, site);
        vars.put(TemplateParams.CONTAINER, contatiner);

        String response = getRestTemplate().getForObject(CONTAINER_URL.getUrl(networkId), String.class, vars);
        log.debug("getContainer: " + response);
        Response<Container> c = mapper.readValue(response, entryResponseType(Container.class));
        return c.getEntry();
    }


    public AlfrescoList<Container> getContainers(String networkId, String site)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getContainers(networkId, site, null);
    }


    public AlfrescoList<Container> getContainers(String networkId, String site, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, networkId);
        vars.put(TemplateParams.SITE, site);

        String response = getRestTemplate().getForObject(CONTAINERS_URL.getUrl(networkId) + generateQueryString(parameters), String.class, vars);
        log.debug("getContainers: " + response);
        Response<Container> c = mapper.readValue(response, entryResponseType(Container.class));
        return c.getList();
    }


    public Member getMember(String networkId, String site, String person)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, networkId);
        vars.put(TemplateParams.SITE, site);
        vars.put(TemplateParams.MEMBER, person);

        String response = getRestTemplate().getForObject(MEMBER_URL.getUrl(networkId), String.class, vars);
        log.debug("getMember: " + response);
        Response<Member> m = mapper.readValue(response, entryResponseType(Member.class));
        return m.getEntry();
    }


    public AlfrescoList<Member> getMembers(String network, String site)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getMembers(network, site, null);
    }


    public AlfrescoList<Member> getMembers(String network, String site, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.SITE, site);

        String response = getRestTemplate().getForObject(MEMBERS_URL + generateQueryString(parameters), String.class, vars);
        log.debug("getMembers: " + response);
        Response<Member> m = mapper.readValue(response, entryResponseType(Member.class));
        return m.getList();
    }


    public Member addMember(String network, String site, String personId, Role role)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.SITE, site);

        Member member = new Member();
        member.setId(personId);
        member.setRole(role);

        String response = getRestTemplate().postForObject(MEMBERS_URL.getUrl(network), new HttpEntity<Member>(member, headers), String.class, vars);
        log.debug("addMember: " + response);
        Response<Member> m = mapper.readValue(response, entryResponseType(Member.class));
        return m.getEntry();
    }


    // TODO should this make the additional call to get the updated entity or just move forward
    public void updateMember(String network, String site, String personId, Role role)
        throws RestClientException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.SITE, site);
        vars.put(TemplateParams.MEMBER, personId);

        Member member = new Member();
        member.setRole(role);

        getRestTemplate().put(MEMBER_URL.getUrl(network), new HttpEntity<Member>(member, headers), vars);
        log.debug("updateMember: member: " + personId + " to Role: " + role);

    }


    // TODO should this make the additional call to get the updated entity or just move forward
    public void deleteMember(String network, String site, String personId)
        throws RestClientException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.SITE, site);
        vars.put(TemplateParams.MEMBER, personId);

        getRestTemplate().delete(MEMBER_URL.getUrl(network), vars);
        log.debug("deleteMember: " + personId + " from site: " + site);

    }


    public Person getPerson(String network, String person)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);

        String response = getRestTemplate().getForObject(PEOPLE_URL.getUrl(network), String.class, vars);
        log.debug("getPerson: " + response);
        Response<Person> p = mapper.readValue(response, entryResponseType(Person.class));
        return p.getEntry();
    }


    public AlfrescoList<Site> getSites(String network, String person)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getSites(network, person, null);
    }


    public AlfrescoList<Site> getSites(String network, String person, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);

        String response = getRestTemplate().getForObject(PEOPLE_SITES_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getSites: " + response);
        Response<Site> s = mapper.readValue(response, entryResponseType(Site.class));
        return s.getList();
    }


    public Site getSite(String network, String person, String site)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {

        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);
        vars.put(TemplateParams.SITE, site);

        String response = getRestTemplate().getForObject(PEOPLE_SITE_URL.getUrl(network), String.class, vars);
        log.debug("getSite: " + response);
        Response<Site> s = mapper.readValue(response, entryResponseType(Site.class));
        return s.getEntry();
    }

    
    public Site addFavoriteSite(String network, String personId, String siteId)
            throws JsonParseException,
                JsonMappingException,
                IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, personId);

        Site site = new Site();
        site.setId(siteId);

        String response = getRestTemplate().postForObject(PEOPLE_FAVORITE_SITES_URL.getUrl(), new HttpEntity<Site>(site, headers), String.class, vars);
        log.debug("addFavoriteSite: " + response);
        Response<Site> c = mapper.readValue(response, entryResponseType(Site.class));
        return c.getEntry();
    }
    

    public AlfrescoList<Site> getFavoriteSites(String network, String person)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getFavoriteSites(network, person, null);
    }


    public AlfrescoList<Site> getFavoriteSites(String network, String person, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);

        String response = getRestTemplate().getForObject(PEOPLE_FAVORITE_SITES_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getFavoriteSites: " + response);
        Response<Site> s = mapper.readValue(response, entryResponseType(Site.class));
        return s.getList();
    }


    public Preference getPreference(String network, String person, String preference)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);
        vars.put(TemplateParams.PREFERENCE, preference);

        String response = getRestTemplate().getForObject(PEOPLE_PREFERENCE_URL.getUrl(network), String.class, vars);
        log.debug("getPreference: " + response);
        Response<Preference> p = mapper.readValue(response, entryResponseType(Preference.class));
        return p.getEntry();
    }


    public AlfrescoList<Preference> getPreferences(String network, String person)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getPreferences(network, person, null);
    }


    public AlfrescoList<Preference> getPreferences(String network, String person, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);

        String response = getRestTemplate().getForObject(PEOPLE_PREFERENCES_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getPreferences: " + response);
        Response<Preference> p = mapper.readValue(response, entryResponseType(Preference.class));
        return p.getList();
    }


    public Network getNetwork(String network, String person)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);

        String response = getRestTemplate().getForObject(PEOPLE_NETWORK_URL.getUrl(network), String.class, vars);
        log.debug("getNetwork: " + response);
        Response<Network> n = mapper.readValue(response, entryResponseType(Network.class));
        return n.getEntry();
    }


    public AlfrescoList<Network> getNetworks(String network, String person)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getNetworks(network, person, null);
    }


    public AlfrescoList<Network> getNetworks(String network, String person, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);

        String response = getRestTemplate().getForObject(PEOPLE_NETWORKS_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getNetworks: " + response);
        Response<Network> n = mapper.readValue(response, entryResponseType(Network.class));
        return n.getList();
    }


    public AlfrescoList<Activity> getActivities(String network, String person)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getActivities(network, person, null);
    }


    public AlfrescoList<Activity> getActivities(String network, String person, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.PERSON, person);

        String response = getRestTemplate().getForObject(PEOPLE_ACTIVITIES_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getActivities: " + response);
        Response<Activity> a = mapper.readValue(response, entryResponseType(Activity.class));
        return a.getList();
    }


    public Tag getTag(String network, String tag)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        int count = 0;
        final int MAXITEMS = 10;

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(Pagination.MAXITEMS, Integer.toString(MAXITEMS));

        boolean found = false;
        Tag tagFound = null;

        AlfrescoList<Tag> response = getTags(network, parameters);
        while (!found)
        {
            for (Iterator<Tag> iterator = response.getEntries().iterator(); iterator.hasNext();)
            {
                Tag _tag = iterator.next();
                if (_tag.getTag().equals(tag))
                {
                    tagFound = _tag;
                    found = true;
                    break;
                }
                else
                {
                    if (!iterator.hasNext())
                    {
                        if (response.getPagination().isHasMoreItems())
                        {
                            parameters.put(Pagination.SKIPCOUNT, Integer.toString(count = count + MAXITEMS));
                            response = getTags(network, parameters);
                        }
                        else
                        {
                            found = true;
                            break;
                        }
                    }
                }
            }
        }
        return tagFound;
    }


    public AlfrescoList<Tag> getTags(String network)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getTags(network, null);
    }


    public AlfrescoList<Tag> getTags(String network, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = Collections.singletonMap(TemplateParams.NETWORK, network);

        String response = getRestTemplate().getForObject(TAGS_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getTags: " + response);
        Response<Tag> t = mapper.readValue(response, entryResponseType(Tag.class));
        return t.getList();
    }


    public void updateTag(String network, String tagId, String tag)
        throws RestClientException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.TAG, tagId);

        Tag _tag = new Tag();
        _tag.setTag(tag);

        getRestTemplate().put(TAG_URL.getUrl(network), new HttpEntity<Tag>(_tag, headers), vars);
        log.debug("updateTag: " + tag);

    }


    public AlfrescoList<Comment> getComments(String network, String node)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getComments(network, node, null);
    }


    public AlfrescoList<Comment> getComments(String network, String node, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        String response = getRestTemplate().getForObject(NODE_COMMENTS_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getComments: " + response);
        Response<Comment> c = mapper.readValue(response, entryResponseType(Comment.class));
        return c.getList();
    }


    public Comment createComment(String network, String node, String comment)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        Comment _comment = new Comment();
        _comment.setContent(comment);

//        String response = getRestTemplate().postForObject(NODE_COMMENTS_URL.getUrl(network), new HttpEntity<Comment>(_comment, headers), String.class, vars);
        String response = getRestTemplate().postForObject(NODE_COMMENTS_URL.getUrl(), new HttpEntity<Comment>(_comment, headers), String.class, vars);
        log.debug("createComment: " + response);
        Response<Comment> c = mapper.readValue(response, entryResponseType(Comment.class));
        return c.getEntry();

    }


    public AlfrescoList<Comment> createComments(String network, String node, java.util.List<String> comments)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        java.util.List<Comment> _comments = new ArrayList<Comment>();
        for (String content : comments)
        {
            Comment _comment = new Comment();
            _comment.setContent(content);
            _comments.add(_comment);
        }

        String response = getRestTemplate().postForObject(NODE_COMMENTS_URL.getUrl(network), new HttpEntity<java.util.List<Comment>>(_comments, headers), String.class, vars);
        log.debug("createComments: " + response);
        Response<Comment> c = mapper.readValue(response, entryResponseType(Comment.class));
        return c.getList();
    }


    public void updateComment(String network, String node, String commentId, String comment)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);
        vars.put(TemplateParams.COMMENT, commentId);

        Comment _comment = new Comment();
        _comment.setContent(comment);

        getRestTemplate().put(NODE_COMMENT_URL.getUrl(network), new HttpEntity<Comment>(_comment, headers), vars);
        log.debug("updateComment: " + comment);
    }


    public void deleteComment(String network, String node, String commentId)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);
        vars.put(TemplateParams.COMMENT, commentId);

        getRestTemplate().delete(NODE_COMMENT_URL.getUrl(network), vars);
        log.debug("deleteComment: " + commentId);
    }


    public AlfrescoList<Tag> getNodesTags(String network, String node)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getNodesTags(network, node, null);
    }


    public AlfrescoList<Tag> getNodesTags(String network, String node, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        String response = getRestTemplate().getForObject(NODE_TAGS_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getNodeTafs: " + response);
        Response<Tag> t = mapper.readValue(response, entryResponseType(Tag.class));
        return t.getList();
    }


    public Tag addTagToNode(String network, String node, String tag)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        Tag _tag = new Tag();
        _tag.setTag(tag);

        String response = getRestTemplate().postForObject(NODE_TAG_URL.getUrl(network), new HttpEntity<Tag>(_tag, headers), String.class, vars);
        log.debug("addTagToNode: " + response);
        Response<Tag> t = mapper.readValue(response, entryResponseType(Tag.class));
        return t.getEntry();
    }


    public AlfrescoList<Tag> addTagsToNode(String network, String node, java.util.List<String> tags)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        java.util.List<Tag> _tags = new ArrayList<Tag>();
        for (String tag : tags)
        {
            Tag _tag = new Tag();
            _tag.setTag(tag);
            _tags.add(_tag);
        }

        String response = getRestTemplate().postForObject(NODE_TAGS_URL.getUrl(network), new HttpEntity<java.util.List<Tag>>(_tags, headers), String.class, vars);
        log.debug("addTagsToNode: " + response);
        Response<Tag> t = mapper.readValue(response, entryResponseType(Tag.class));
        return t.getList();
    }


    public void removeTagFromNode(String network, String node, String tagId)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);
        vars.put(TemplateParams.TAG, tagId);

        getRestTemplate().delete(NODE_TAG_URL.getUrl(network), vars);
        log.debug("removeTagFromNode: " + tagId);
    }


    public AlfrescoList<Rating> getNodeRatings(String network, String node)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        return getNodeRatings(network, node, null);
    }


    public AlfrescoList<Rating> getNodeRatings(String network, String node, Map<String, String> parameters)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        String response = getRestTemplate().getForObject(NODE_RATINGS_URL.getUrl(network) + generateQueryString(parameters), String.class, vars);
        log.debug("getNodeRatings: " + response);
        Response<Rating> r = mapper.readValue(response, entryResponseType(Rating.class));
        return r.getList();
    }


    public Rating getNodeRating(String network, String node, String rating)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);
        vars.put(TemplateParams.RATING, rating);

        String response = getRestTemplate().getForObject(NODE_RATING_URL.getUrl(network), String.class, vars);
        log.debug("getNodeRatings: " + response);
        Response<Rating> r = mapper.readValue(response, entryResponseType(Rating.class));
        return r.getEntry();
    }


    public void removeNodeRating(String network, String node, String ratingId)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);
        vars.put(TemplateParams.RATING, ratingId);

        getRestTemplate().delete(NODE_RATING_URL.getUrl(network), vars);
        log.debug("removeNodeRating: " + ratingId);
    }


    /*
     * public Rating rateNode(String network, String node, String ratingType, Serializable rating) throws JsonParseException,
     * JsonMappingException, IOException { Map<String, String> vars = new HashMap<String, String>();
     * vars.put(TemplateParams.NETWORK, network); vars.put(TemplateParams.NODE, node);
     * 
     * Rating _rating = new Rating(); _rating.setId(ratingType); _rating.setMyRating(rating);
     * 
     * String response = getRestTemplate().postForObject(NODE_RATINGS_URL, new HttpEntity<Rating>(_rating, headers), String.class,
     * vars); log.debug("rateNode: " + response); Response<Rating> r = mapper.readValue(response, entryResponseType(Rating.class));
     * return r.getEntry(); }
     */

    public Rating rateNode(String network, String node, boolean like)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        Rating _like = new Rating();
        _like.setId(Rating.LIKES);
        _like.setMyRating(like);

        String response = getRestTemplate().postForObject(NODE_RATINGS_URL.getUrl(network), new HttpEntity<Rating>(_like, headers), String.class, vars);
        log.debug("rateNode: " + response);
        Response<Rating> r = mapper.readValue(response, entryResponseType(Rating.class));
        return r.getEntry();
    }


    public Rating rateNode(String network, String node, int stars)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(TemplateParams.NETWORK, network);
        vars.put(TemplateParams.NODE, node);

        Rating _stars = new Rating();
        _stars.setId(Rating.STARS);
        _stars.setMyRating(stars);

        String response = getRestTemplate().postForObject(NODE_RATINGS_URL.getUrl(network), new HttpEntity<Rating>(_stars, headers), String.class, vars);
        log.debug("rateNode: " + response);
        Response<Rating> r = mapper.readValue(response, entryResponseType(Rating.class));
        return r.getEntry();
    }


    private JavaType entryResponseType(Class<?> type)
    {
        return mapper.getTypeFactory().constructParametricType(Response.class, type);
    }

    public Network getHomeNetwork()
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        Network homeNetwork = null;
        AlfrescoList<Network> response = getNetworks();

        for (Iterator<Network> iterator = response.getEntries().iterator(); iterator.hasNext();)
        {
            Network network = iterator.next();

            if (network.isHomeNetwork())
                homeNetwork = network;
        }
        return homeNetwork;
    }


    public Person getCurrentUser()
        throws JsonParseException,
            JsonMappingException,
            IOException
    {       
        return getPerson(getHomeNetwork().getId(), "-me-");
    }

    /**
     * Not Implemented yet
     */
    @Deprecated
    public AlfrescoList<Metadata> networkOptions(String network)
        throws JsonParseException,
            JsonMappingException,
            IOException
    {
        throw new OperationNotPermittedException("HTTP OPTIONS Not implemented yet");
        // Map<String, String> vars = Collections.singletonMap(TemplateParams.NETWORK, network);
        // ResponseEntity<String> response =
        // getRestTemplate().exchange("https://api.alfresco.com/{network}/public/alfresco/versions/1/nodes", HttpMethod.OPTIONS,
        // null, String.class, vars);
        // System.out.println("Network Options: " + response.getBody());
        // return mapper.readValue(response.getBody(), entryResponseType(Metadata.class));

    }
    
    
    private JavaType registrationResponseType(Class<?> type)
    {
        return mapper.getTypeFactory().constructParametricType(Response.class, type);
    }
    
	public UserRegistration registerUser(String email, String firstName, String lastName, String password, String source, String sourceUrl)
			throws IOException
	{
		int idx = email.indexOf("@");
		if(idx == -1)
		{
			throw new RuntimeException("Invalid user id");
		}
		String networkId = email.substring(idx + 1);
		Map<String, String> vars = new HashMap<String, String>();
		vars.put(TemplateParams.NETWORK, networkId);

		UserRegistrationRequest userRegistration = new UserRegistrationRequest();
		userRegistration.setEmail(email);
		userRegistration.setFirstName(firstName);
		userRegistration.setLastName(lastName);
		userRegistration.setPassword(password);
		userRegistration.setSource(source);
		userRegistration.setSourceUrl(sourceUrl);

		/*
		 * {
    "registration" :    {
      "email": "joe.bloggs@mingmong",
      "registrationDate": "2012-11-13T14:08:37.000Z",
      "id": "activiti$20078",
      "key": "fe76ca23-3dbb-40ff-a20c-c19b0da46f85"
   }

}
		 */
		String response = getRestTemplate().postForObject(REGISTER_USER_URL.getUrl(), new HttpEntity<UserRegistrationRequest>(userRegistration, headers), String.class, vars);
		log.debug("registerUser: " + response);
		UserRegistrationResponse r = mapper.readValue(response, UserRegistrationResponse.class);
		return r.getUserRegistration();
	}

	public UserActivationResponse activateUser(String id, String key, String email, String firstName, String lastName, String password)
			throws IOException
	{
		Map<String, String> vars = new HashMap<String, String>();

		UserActivationRequest userActivation = new UserActivationRequest();
		userActivation.setEmail(email);
		userActivation.setFirstName(firstName);
		userActivation.setLastName(lastName);
		userActivation.setPassword(password);
		userActivation.setId(id);
		userActivation.setKey(key);

		String response = getRestTemplate().postForObject(ACTIVATE_USER_URL.getUrl(), new HttpEntity<UserActivationRequest>(userActivation, headers), String.class, vars);
		log.debug("activateUser: " + response);		
		UserActivationResponse r = mapper.readValue(response, UserActivationResponse.class);
		return r;

	}
	
	public LegacySite createSite(String network, String siteId, String sitePreset, String title, String description, Visibility visibility)
			throws IOException
	{
		Map<String, String> vars = new HashMap<String, String>();
		vars.put(TemplateParams.NETWORK, network);

		LegacySite _site = new LegacySite();
		_site.setShortName(siteId);
		_site.setSitePreset(sitePreset);
		_site.setTitle(title);
		_site.setDescription(description);
        _site.setVisibility(visibility.toString());

		String response = getRestTemplate().postForObject(CREATE_SITE_URL.getUrl(network), new HttpEntity<LegacySite>(_site, headers), String.class, vars);
		log.debug("createSite: " + response);
		LegacySite resp = mapper.readValue(response, LegacySite.class);
		return resp;
	}
	
	public ObjectId createRelationship(String networkId, String sourceObjectId, String targetObjectId)
	{
		Session session = getCMISSession(networkId);

		Map<String, Serializable> relProps = new HashMap<String, Serializable>(); 
		relProps.put("cmis:sourceId", sourceObjectId); 
		relProps.put("cmis:targetId", targetObjectId); 
		relProps.put("cmis:objectTypeId", "cmis:relationship"); 
		ObjectId res = session.createRelationship(relProps);
		return res;
	}

	public java.util.List<Tree<FileableCmisObject>> getDescendants(String networkId, String folderId, Integer depth, IncludeRelationships includeRelationships,
			Boolean includeAcls, Set<String> propertyFilter, Boolean includePolicies)
	{
		Session session = getCMISSession(networkId);

		CmisObject o = session.getObject(folderId);
		if(o instanceof Folder)
		{
			Folder f = (Folder)o;

			OperationContext ctx = new OperationContextImpl();
			ctx.setIncludeAcls(false);
			ctx.setIncludeRelationships(includeRelationships);
			ctx.setIncludeAcls(includeAcls);
			ctx.setFilter(propertyFilter);
			ctx.setIncludePolicies(includePolicies);

			java.util.List<Tree<FileableCmisObject>> ret = f.getDescendants(depth, ctx);
			return ret;
		}
		else
		{
			throw new IllegalArgumentException("Folder does not exist or is not a folder");
		}
	}
	
	public ItemIterable<CmisObject> getChildren(String networkId, String folderId, int skipCount, int maxItems, IncludeRelationships includeRelationships,
			Boolean includeAcls, Set<String> propertyFilter, Boolean includePolicies)
	{
		Session session = getCMISSession(networkId);

		CmisObject o = session.getObject(folderId);
		if(o instanceof Folder)
		{
			Folder f = (Folder)o;
			
			OperationContext ctx = new OperationContextImpl();
			ctx.setIncludeAcls(false);
			ctx.setIncludeRelationships(includeRelationships);
			ctx.setIncludeAcls(includeAcls);
			ctx.setFilter(propertyFilter);
			ctx.setIncludePolicies(includePolicies);
			
			ItemIterable<CmisObject> res = f.getChildren(ctx);
			res.skipTo(skipCount);
			ItemIterable<CmisObject> ret = res.getPage(maxItems);
			return ret;
		}
		else
		{
			throw new IllegalArgumentException("Folder does not exist or is not a folder");
		}
	}
	
//	public List<Person> getPeople() throws PublicApiException
//	{
//		RequestContext context = getRequestContext();
//		if(rc == null)
//		{
//			throw new RuntimeException("Must set a request context");
//		}
//		
//        String URL = PUBLIC_API_BASEURL + "people";
//
//        String networkId = context.getNetworkId();
//        String username = context.getRunAsUser();
//        
//        GetMethod getMethod = new GetMethod(httpProvider.getFullAlfrescoUrlForPath(MessageFormat.format(URL, networkId)));
//
//        HttpResponse httpResponse = authenticatedHttp.executeHttpMethodAuthenticated(getMethod, username, getCallback("Error getting people"));
//        if (HttpServletResponse.SC_OK != httpResponse.getStatusCode())
//        {
//            String msg = "Failed to get people: \n" +
//                    "   Response: " + httpResponse;
//            throw new PublicApiException(msg, httpResponse);
//        }
//        else
//        {
//        	List<Person> people = Person.parsePeople(httpResponse.getJsonResponse());
//            return people;
//        }
//	}

	/**
	 * Build QueryString
	 * 
	 * @param parameters
	 * @return
	 */
	private String generateQueryString(Map<String, String> parameters)
	{
		StringBuilder queryString = new StringBuilder();
		if (parameters != null && parameters.size() > 0)
		{
			queryString.append("?");

			Iterator<java.util.Map.Entry<String, String>> entries = parameters.entrySet().iterator();
			while (entries.hasNext())
			{
				java.util.Map.Entry<String, String> thisEntry = entries.next();
				queryString.append(thisEntry.getKey()).append("=").append(thisEntry.getValue());
				if (entries.hasNext())
				{
					queryString.append("&");
				}
			}
		}

		log.debug("queryString: " + queryString.toString());
		return queryString.toString();
	}

	private static class TemplateParams
	{
		public final static String NETWORK    = "network";
		public final static String SITE       = "site";
		public final static String CONTAINER  = "container";
		public final static String PREFERENCE = "preference";
		public final static String TAG        = "tag";
		public final static String RATING     = "rating";
		public final static String COMMENT    = "comment";
		public final static String NODE       = "node";
		public final static String PERSON     = "person";
		public final static String MEMBER     = "member";
	}

	public static class QueryParams
	{
		public final static String PROPERTIES = "properties";
	}

	private class CMISSessions
	{
		private Map<String, Session> sessions = new HashMap<String, Session>();

		Session getSession(String networkId)
		{
			Session session = sessions.get(networkId);
			if(session == null)
			{
				session = createCMISSession(networkId);
				sessions.put(networkId, session);
			}
			return session;
		}
	}

	protected interface PublicApiUrl
	{
		String getUrl();
		String getUrl(String networkId);
	}
	
	protected abstract class AbstractPublicApiNetworkUrl implements PublicApiUrl
	{
		protected String baseUrl;
		protected String url;

		AbstractPublicApiNetworkUrl(String baseUrl, String url)
		{
			this.baseUrl = baseUrl;
			this.url = url;
		}
	}

	protected class PublicApiNetworkUrl extends AbstractPublicApiNetworkUrl
	{
		PublicApiNetworkUrl(String baseUrl, String url)
		{
			super(baseUrl, url);
		}

		public String getUrl()
		{
			return baseUrl + (production ? "" : "a/") + url;
		}

		public String getUrl(String networkId)
		{
			return baseUrl + (production ? "" : "a/") + url.replaceAll("\\{network\\}", networkId);
		}
	}
	
	protected class PublicApiServiceUrl extends AbstractPublicApiNetworkUrl
	{
		public PublicApiServiceUrl(String baseUrl, String url)
		{
			super(baseUrl, url);
		}

		public String getUrl()
		{
			return baseUrl + (production ? "" : "service/") + url;
		}

		public String getUrl(String networkId)
		{
			throw new UnsupportedOperationException();
		}
	}
}