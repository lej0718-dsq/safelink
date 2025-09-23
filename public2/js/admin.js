$(document).ready(function () {
	$('#headerPanel').load('./admin_menu.html #headerPanel', function(response, status, xhr) { 
		if (status === "success") { // 외부 콘텐츠가 로드된 후 DOM 요소를 조작 
			$('#headerPanel #loginid').text(korname+"(" + username +")"); 
			updateClock();
			setInterval(updateClock, 1000); // 매초마다 updateClock 함수 실행
		} else { 
			console.error("Failed to load HTML:", status, xhr.statusText); 
		} 
	});

	$("#footerPanel").load("./admin_menu.html #footerPanel");
    if(group === 'admin') {
        $("#menuPanel").load("./admin_menu.html #menuPanel");
    } else {
        $("#menuPanel").load("./admin_menu.html #menuPanelGroup");
    }
  });
  
var token = sessionStorage.getItem("token");
var username = sessionStorage.getItem("username");
var korname = sessionStorage.getItem("korname");
var group = sessionStorage.getItem("group");
console.log(token);

function formatStatus(code) {
	if (code=="A") {
		return "정상";
	} else if (code == "D") {
		return "해지";
	}
}

function formatOffercode(code) {
	if (code=="11") {
		return "OHC";
	} else if (code=="12") {
		return "Buzzvil";
	} else if (code=="91") {
		return "ARS연계";
	} else if (code=="00") {
		return "홈페이지";
	} else if (code == null || code=="") {
		return "NaN";
	} else {
		return code;
	}
}

function formatDate(dateString) {
	if (dateString == "" || dateString == null) return "";
    const date = new Date(dateString.replaceAll(" KST ", " "));
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

function setLoginid(t) {
	$('#loginid').text(t);
}

//base64 디코딩 함수 
  function base64Decode(str) { 
  	return decodeURIComponent(atob(str).split('').map(
  			function(c) { 
  				return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2); }
  			).join('')
  	);
  }

  //JWT 클레임 정보 추출 함수 
  function getJwtClaims(token) {
  	const parts = token.split('.'); 
  	if (parts.length !== 3) { 
  		throw new Error('Invalid JWT token'); 
  	} 
  	const claimsBase64 = parts[1]; // 두 번째 부분이 클레임 
  	const claimsJson = base64Decode(claimsBase64); 
  	return JSON.parse(claimsJson);
  }

  //username 클레임 추출 함수 
  function getTextFromToken(claim_name, token) { 
  	const claims = getJwtClaims(token); 
  	return claims[claim_name];
  } 
  
  function updateClock() {
      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 +1 필요
      const day = String(now.getDate()).padStart(2, '0');
      const hours = String(now.getHours()).padStart(2, '0');
      const minutes = String(now.getMinutes()).padStart(2, '0');
      const seconds = String(now.getSeconds()).padStart(2, '0');
      
      // 원하는 형식으로 시간 문자열 생성
      const currentTime = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      
      // jQuery로 시간 업데이트
      $('#clock').text(currentTime);
  }
  