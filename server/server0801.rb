require 'bundler/setup'
require 'sinatra'
require 'oauth2'
require 'json'
require "net/http"
require "uri"

# Our client ID and secret, used to get the access token
CLIENT_ID = "03e1688e-5865-4077-a8a9-5fa609328573"
CLIENT_SECRET = "226dd0af-1132-4b15-acfd-7aaeb915642f"

# We'll store the access token in the session
use Rack::Session::Pool, :session_only => false
#use Rack::Session::Cookie, :session_only => false

# This is the URI that will be called with our access
# code after we authenticate with out SmartThings account
redirect_uri = 'http://localhost:4567/oauth/callback'

# This is the URI we will use to get the endpoints once we've received our token
endpoints_uri = 'https://graph.api.smartthings.com/api/smartapps/endpoints'

options = {
    site: 'https://graph.api.smartthings.com/api/smartapps/endpoints',
    authhorize_url: '/oauth/authorize',
    token_url: '/oauth/token'
}

# use the OAuth2 module to handle OAuth flow
client = OAuth2::Client.new(CLIENT_ID, CLIENT_SECRET, options)

def authenticated?
    session[:access_token]
end

# handle requests to the application root
get '/' do
    
    if !authenticated?
        redirect '/authorize'
        else
        redirect '/hello'
        #redirect '/authorize'
    end
end

# handle requests to /authorize URL
get '/authorize' do
    # Use the OAuth2 module to get the authorize URL.
    # After we authenticate with SmartThings, we will be redirected to the
    # redirect_uri, including our access code used to get the token
    url = client.auth_code.authorize_url(redirect_uri: redirect_uri, scope: 'app')
    redirect url
end

# handle requests to /oauth/callback URL. We will tell SmartThings
# to call this URL with our authorization code once we've authenticated
get '/oauth/callback' do
    # The callback is called with a "code" URL parameter
    # This is the code we can use to get our access token
    code = params[:code]
    
    puts 'headers: ' + headers.to_hash.inspect
    
    # Use the code to get the token
    response = client.auth_code.get_token(code, redirect_uri: redirect_uri, scope: 'app')
    
    # now that we have the access token, we will store it in the session
    session[:access_token] = response.token
    
    # debug - inspect the running console for the
    # expires in (seconds from now), and the expires at (in epoch time)
    puts 'TOKEN EXPIRES IN ' + response.expires_in.to_s
    puts 'TOKEN EXPIRES AT ' + response.expires_at.to_s
    redirect '/hello'
end

# handle requests to the /getSwitch URL. This is where we will make
# requests to gert information about the configured switch
get '/hello' do
    # If we get to this URL without having gotten the access token
    # redirect back to root to go through authorization
    
    #store the token in the cookie more permanent
    token = session[:access_token]
    
    # make a request to the SmartThings endpoint URI, using the token
    # to get our endpoints
    url = URI.parse(endpoints_uri)
    req = Net::HTTP::Get.new(url.request_uri)
    
    # we set a HTTP header of "Authorization: Bearer <API Token>"
    req['Authorization'] = 'Bearer ' + token
    
    http = Net::HTTP.new(url.host, url.port)
    http.use_ssl = (url.scheme == "https")
    
    response = http.request(req)
    json = JSON.parse(response.body)
    
    # debug statement
    puts json
    
    # get the endpoint from the JSON:
    uri = json[0]['uri']
    
    
    puts 'Unlock the door with pin code'
    
    lockUrl = uri + '/code/9824'
    getlockURL = URI.parse(lockUrl)
    getlockReq = Net::HTTP::Put.new(getlockURL.request_uri)
    getlockReq['Authorization'] = 'Bearer ' + token
    getlockHttp = Net::HTTP.new(getlockURL.host, getlockURL.port)
    getlockHttp.use_ssl = true
    
    lockStatus = getlockHttp.request(getlockReq)
    
    
    puts 'declare the door status'
    
    doorUrl = uri+ '/keys'
    getdoorURL = URI.parse(doorUrl)
    getdoorReq = URI.parse(doorUrl)
    getdoorReq = Net::HTTP::Get.new(getdoorURL.request_uri)
    getdoorReq['Authorization'] = 'Bearer' + token
    getdoorHttp = Net::HTTP.new(getdoorURL.host,getdoorURL.port)
    getdoorHttp.use_ssl = true
    
    doorStatus = getdoorHttp.request(getdoorReq)
    
    '<h3>Response Code</h3>' + doorStatus.body

end
