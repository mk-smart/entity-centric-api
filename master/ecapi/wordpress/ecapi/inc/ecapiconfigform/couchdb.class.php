<?php

// Based on http://wiki.apache.org/couchdb/Getting_started_with_PHP

class couchdb {

    function couchdb($cburl, $db, $user, $pass) {
       $this->cburl = $cburl;
       $this->db    = $db;       
       $this->user  = $user;
       $this->pass  = $pass;
    } 
   
	function getDoc($id, $path = '/') {
		$ch = curl_init(); 
		curl_setopt($ch, CURLOPT_URL, "{$this->cburl}/{$this->db}$path" . urlencode($id));
		curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'GET');
		// curl_setopt($ch, CURLOPT_HEADER, TRUE);
		curl_setopt($ch, CURLOPT_HTTPHEADER, array(
			'Accept: application/json'
		));
		curl_setopt($ch, CURLOPT_USERPWD, "{$this->user}:{$this->pass}"); 
		curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
		$response = curl_exec($ch);
		$error = curl_error($ch);
		curl_close($ch);
		return array('data' => json_decode($response),'error' => $error);
	}

   function saveDoc($id, $json_obj){
      $ch = curl_init(); 
		curl_setopt($ch, CURLOPT_URL, "{$this->cburl}/{$this->db}/" . urlencode($id));
      curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT'); 
      curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($json_obj));
      curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
      curl_setopt($ch, CURLOPT_HTTPHEADER, array(
		 'Content-type: application/json',
		 'Accept: application/json'
      )); 
      curl_setopt($ch, CURLOPT_USERPWD, $this->user.':'.$this->pass); 
      curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
      $response = curl_exec($ch); 
$error = curl_error($ch);
     $info = curl_getinfo($ch);
      curl_close($ch);
      return array('data' => json_decode($response),'error' => $error, 'info' => $info);
   }
   
}
