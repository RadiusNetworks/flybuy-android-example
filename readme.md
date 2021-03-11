# flybuy-android-example

Sample Application demonstrating the following features using the FlyBuy SDK
1.  Order redemption via url or code
2.  FlyBuy customer management
3.  Location permissions request
4.  Customer journey updates via LiveData streams
5.  Push notifications via Firebase
6.  Customer rating on pickup experience

This example uses the `Example Apps` project in the FlyBuy Merchant portal, which uses the white label domain `demo-flybuy.radiusnetworks.com` to send redemption links. 

### Adding Your Token
To use this repo, you must enter your FlyBuy token on line 17 of `ExampleApplication.kt`. Contact your project manager if you do not have a token.

**Firebase push notifications**
This example uses Google's Firebase push notifications.  You need to provide a `google-services.json` file containing the API key, etc. in the `app/` folder.



#### On startup
 1. Initialize the FlyBuy SDK  
 3. Check for existing open orders  
    - When open orders exist, pick the first one and jump to the GuestJourney activity  
 4. Check for an intent indicating the app was opened via a URL.  
    - Get the redemption code and go to the OnMyWay activity  
 5. Wait for user to enter a redemption code
     
**On Redemption Code lookup**  
 1. Check for current FlyBuy customer, create one if does not exist 
    - Show terms and conditions before creating customer
 2. Fetch order details
 3. Display current customer info (phone / vehicle) to allow updating  
 4. Wait for user to press `On My Way`
 
 **On My Way**  
 1. Update customer information if changed  
 2. Check for Location permissions and request if necessary.  
 3. Go to GuestJourney activity
     
**Guest Journey**  
 1. Set up LiveData observers to receive updates for active order  
    - OrderState changes to `cancelled` or `gone` go to MainActivity
    - order.open() returns `false` go to OrderComplete activity
    - CustomerState changes to `arrived`, update the progress bar and display pickup instructions  
    - CustomerState changes to `waiting` update the progress bar and show `I'm done` button  
    - CustomerState changes to `en_route`, `nearby`, update progress bar and ETA  
 2. Claim active order if not already claimed  
 3. On click `I'm here`, set the CustomerState to `waiting`
 4. On click `I'm done`, set the CustomerState to `completed`

  **Order Complete**  
 1. Allow user to rate pickup experience
 2. Return to MainActivity
