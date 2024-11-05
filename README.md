[11/5, 11:20] Me: // Helper method to generate a unique key for each week based on the date
    private String getWeekKey(Date activityDate) {
        Integer weekNumber =  getWeekNumber(activityDate);
        return activityDate.year() + '-' + activityDate.month() + '-W'+weekNumber;
    }
    
    // Helper method to get the start of the week (based on Sunday as start day)
    private Date getWeekStart(Date activityDate) {
        return activityDate.toStartOfWeek();
    }

    // Helper method to get the end of the week (based on Saturday as end day)
    private Date getWeekEnd(Date activityDate) {
        return activityDate.toStartOfWeek().addDays(6);
    }

    private static Integer getWeekNumber(Date inputDate) {
        Datetime inputDatetime = Datetime.newInstance(inputDate, Time.newInstance(0, 0, 0, 0));
        String weekNumber = inputDatetime.format('W');
        Datetime monthStart = Datetime.newInstance(Date.newInstance(inputDate.year(), inputDate.month(), 1), Time.newInstance(0, 0, 0, 0));
        String weekNumberAtMonthStart = monthStart.format('W');
 
        if (Integer.valueOf(weekNumber) >= Integer.valueOf(weekNumberAtMonthStart)) {
            return Integer.valueOf(weekNumber);
        } else {
            return (Integer)(Integer.valueOf(weekNumber) - Integer.valueOf(weekNumberAtMonthStart));
        }
    }
}
[11/5, 11:21] Me: To handle authentication securely when making callouts from one Salesforce org to another, you can use **OAuth 2.0** with the **Username-Password Flow**. This flow allows you to programmatically obtain an access token, which can then be used in the `Authorization` header of your callout requests.

Here’s how to set up a login method for the calling Salesforce org to authenticate with the target org and obtain an access token.

### Steps to Set Up Authentication Using OAuth 2.0

1. **Set Up a Connected App in the Target Org**:
   - Go to **Setup** in the target org and search for **App Manager**.
   - Click **New Connected App**.
   - Enter details like **Name**, **Contact Email**, and **API (Enable OAuth Settings)**.
   - Under **OAuth Scopes**, add `Full Access (full)` or any other specific access you need.
   - Check **Enable OAuth Settings**, and set the **Callback URL** (use any valid URL, as it won’t be used in the Username-Password Flow).
   - Note down the **Consumer Key** and **Consumer Secret**; these will be used for the OAuth flow.
   - Save the app and wait for a few minutes for it to be available.

2. **Create the Login Method** in the Calling Org:
   - This method will use the `Http` class to make an authentication request to the target Salesforce org's OAuth endpoint.
   - This method will return an access token, which can then be used in callouts to authenticate with the target org.

### Apex Code for OAuth Login Method

Here’s a method to perform the login and retrieve an access token.

```apex
public class SalesforceOAuthLogin {
    // Set up your client ID, client secret, username, and password
    private static final String CLIENT_ID = 'Your_Consumer_Key';
    private static final String CLIENT_SECRET = 'Your_Consumer_Secret';
    private static final String USERNAME = 'your_user@domain.com';
    private static final String PASSWORD = 'your_password_and_security_token'; // Append security token if needed
    private static final String LOGIN_URL = 'https://login.salesforce.com/services/oauth2/token';

    // Method to get the access token
    public static String getAccessToken() {
        Http http = new Http();
        HttpRequest request = new HttpRequest();
        request.setEndpoint(LOGIN_URL);
        request.setMethod('POST');
        
        // Set the body with client ID, client secret, username, and password
        String requestBody = 'grant_type=password'
            + '&client_id=' + CLIENT_ID
            + '&client_secret=' + CLIENT_SECRET
            + '&username=' + USERNAME
            + '&password=' + PASSWORD;
        request.setBody(requestBody);
        request.setHeader('Content-Type', 'application/x-www-form-urlencoded');
        
        try {
            HttpResponse response = http.send(request);
            if (response.getStatusCode() == 200) {
                // Parse the JSON response to extract the access token
                Map<String, Object> result = (Map<String, Object>) JSON.deserializeUntyped(response.getBody());
                return (String) result.get('access_token');
            } else {
                throw new CalloutException('Failed to get access token: ' + response.getStatus() + ' ' + response.getBody());
            }
        } catch (Exception e) {
            throw new CalloutException('Failed to get access token: ' + e.getMessage());
        }
    }
}
```

- **Explanation**:
  - The `requestBody` includes parameters such as `grant_type=password`, `client_id`, `client_secret`, `username`, and `password`.
  - The **password** includes both the user’s password and the security token (if the IP address isn’t whitelisted in the target org).
  - The method sends a POST request to Salesforce's OAuth token endpoint (`https://login.salesforce.com/services/oauth2/token`).
  - Upon successful login, the method parses and returns the `access_token`.

### Using the Access Token in Callouts

Once you obtain the access token, you can pass it in the `Authorization` header of your REST and SOAP callouts.

#### Example Usage in the REST Callout

```apex
public class TaskRestCallout {
    public static String createTaskInOtherOrg(String subject, String whatId, String whoId) {
        String accessToken = SalesforceOAuthLogin.getAccessToken();
        Http http = new Http();
        HttpRequest request = new HttpRequest();
        
        request.setEndpoint('https://targetInstance.salesforce.com/services/apexrest/task');
        request.setMethod('POST');
        
        request.setHeader('Authorization', 'Bearer ' + accessToken);
        request.setHeader('Content-Type', 'application/json');
        
        Map<String, String> requestBody = new Map<String, String>{
            'subject' => subject,
            'whatId' => whatId,
            'whoId' => whoId
        };
        
        request.setBody(JSON.serialize(requestBody));
        
        HttpResponse response = http.send(request);
        
        if (response.getStatusCode() == 200) {
            return response.getBody();
        } else {
            throw new CalloutException('Failed to create Task: ' + response.getStatus() + ' ' + response.getBody());
        }
    }
}
```

#### Example Usage in the SOAP Callout

```apex
public class ContactSoapCallout {
    public static Contact getContactDetailsFromOtherOrg(String contactId) {
        String accessToken = SalesforceOAuthLogin.getAccessToken();
        ContactWebServicePort.ContactWebServicePortService service = new ContactWebServicePort.ContactWebServicePortService();
        
        service.endpoint_x = 'https://targetInstance.salesforce.com/services/Soap/class/ContactWebService';
        
        // Set the session header for SOAP
        service.SessionHeader = new ContactWebServicePort.SessionHeader_element();
        service.SessionHeader.sessionId = accessToken;

        try {
            Contact contact = service.getContactDetails(contactId);
            return contact;
        } catch (Exception e) {
            throw new CalloutException('Failed to retrieve Contact details: ' + e.getMessage());
        }
    }
}
```

### Summary

- **Create a Connected App** in the target Salesforce org for OAuth.
- Use the `SalesforceOAuthLogin` class to obtain an access token.
- Pass the access token in the `Authorization` header for REST or `SessionHeader` for SOAP callouts.

This setup provides a secure way to authenticate and perform callouts from one Salesforce org to another. make callouts from another Salesforce org to the REST and SOAP web services described in the assignments, here’s how to set up and execute each callout:

### Part 1: REST Callout to `TaskRestService`

1. **Set up Remote Site Settings** in the calling Salesforce org to allow HTTP requests to the target org.
2. Write an Apex class in the calling org to send a POST request to the REST service.

#### REST Callout Code
This code snippet makes an HTTP POST request to create a Task record in the target Salesforce org.

```apex
public class TaskRestCallout {
    public static String createTaskInOtherOrg(String subject, String whatId, String whoId) {
        Http http = new Http();
        HttpRequest request = new HttpRequest();
        
        // Replace 'https://targetInstance.salesforce.com' with the correct instance URL of the target org
        request.setEndpoint('https://targetInstance.salesforce.com/services/apexrest/task');
        request.setMethod('POST');
        
        // Replace 'Bearer your_session_id_or_oauth_token' with an actual valid session ID or OAuth token
        request.setHeader('Authorization', 'Bearer your_session_id_or_oauth_token');
        request.setHeader('Content-Type', 'application/json');
        
        // Set the request body
        Map<String, String> requestBody = new Map<String, String>();
        requestBody.put('subject', subject);
        requestBody.put('whatId', whatId);
        requestBody.put('whoId', whoId);
        
        request.setBody(JSON.serialize(requestBody));
        
        // Send the request and capture the response
        HttpResponse response = http.send(request);
        
        if (response.getStatusCode() == 200) {
            // Return the Task ID from the response if the call is successful
            return response.getBody();
        } else {
            // Handle errors by throwing an exception
            throw new CalloutException('Failed to create Task: ' + response.getStatus() + ' ' + response.getBody());
        }
    }
}
```

### Part 2: SOAP Callout to `ContactWebService`

1. **Generate the WSDL for `ContactWebService`**:
   - In the target Salesforce org (where `ContactWebService` is defined), go to **Setup > Apex Classes** and click **Generate WSDL** for the `ContactWebService` class. Save the WSDL file.
2. **Generate the Apex class from WSDL** in the calling Salesforce org:
   - Go to **Setup > Apex Classes**, and click **Generate from WSDL** to upload the WSDL file saved earlier. Salesforce will generate an Apex class (e.g., `ContactWebServicePort`) to interact with the SOAP service.
3. Write an Apex class in the calling org to call the SOAP service and retrieve the contact details.

#### SOAP Callout Code
Here’s the code for calling the SOAP service to retrieve contact details.

```apex
public class ContactSoapCallout {
    public static Contact getContactDetailsFromOtherOrg(String contactId) {
        // Create an instance of the generated SOAP class
        ContactWebServicePort.ContactWebServicePortService service = new ContactWebServicePort.ContactWebServicePortService();
        
        // Set the endpoint URL (update with actual instance URL of the target org)
        service.endpoint_x = 'https://targetInstance.salesforce.com/services/Soap/class/ContactWebService';
        
        // Perform the callout to get contact details
        try {
            Contact contact = service.getContactDetails(contactId);
            return contact; // Return the retrieved contact details
        } catch (Exception e) {
            // Handle any exceptions during the SOAP callout
            throw new CalloutException('Failed to retrieve Contact details: ' + e.getMessage());
        }
    }
}
```

### Notes:
- **Authorization**: Ensure the `Authorization` header has a valid **session ID** or **OAuth access token** for the target Salesforce org.
- **Remote Site Settings**: Both the REST and SOAP endpoints require the calling org to add the target org's URL to **Remote Site Settings**.
- **Testing**: Test these callouts in a sandbox or developer environment to ensure they work before deploying them in production.

With these steps, you’ll have set up callouts from one Salesforce org to the REST and SOAP services in another Salesforce org.
