To make callouts from another Salesforce org to the REST and SOAP web services described in the assignments, here’s how to set up and execute each callout:

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
