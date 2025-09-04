$(document).ready(function () {

    //반응형 초기화
    winH = 0
    winW = 0
    var delay = 500;

    //높이,너비를 윈도우의 창의 높이로 설정
    winH = $(window).innerHeight();
    winW = $(window).innerWidth();
    winResize(); //초기값설정

    //반응형 자동크기 설정
    $(window).resize(function () {
        winResize();
    });
    
	//if (confirm ('현재 서비스 점검 중입니다. \n점검시간: 00시 ~ 06시')) 
	//return;

    function winResize() {
        winH = $(window).innerHeight();
        winW = $(window).innerWidth();
    }
	
	function closeWindows() {
		if (confirm("처음 페이지로 돌아갈까요?")) {
			window.location.href = "/index_allone.html";
		}
	}
	
	function LoadingWithMask() {
	    // 화면의 높이와 너비를 구합니다.
	    var maskHeight = $(document).height();
	    var maskWidth = $(window).width();
	    
	    // 화면에 출력할 마스크를 설정해줍니다.
	    var mask = "<div id='mask' style='position:fixed; z-index:9000; background-color:#000000; display:none; left:0; top:0;'></div>";
	    var loadingImg = '';
	    loadingImg += "<div id='loadingImg' style='position: fixed; z-index:10000; display: none;'>";
	    loadingImg += " <img src='./images/spinner-2.gif' style='display: block; border-radius: 50%; box-shadow: 0 0 0 2px #EEE; object-fit: cover;'/>";
	    loadingImg += "</div>";

	    // 화면에 레이어 추가
	    $('body').append(mask).append(loadingImg);

	    // 마스크의 높이와 너비를 화면 것으로 만들어 전체 화면을 채웁니다.
	    $('#mask').css({
	        'width': maskWidth,
	        'height': maskHeight,
	        'opacity': '0.3'
	    });

	    // loadingImg 요소를 화면 중앙에 위치시킵니다.
	    $('#loadingImg').css({
	        'top': '50%',
	        'left': '50%',
	        'transform': 'translate(-50%, -50%)'
	    });

	    // 마스크와 로딩 이미지 표시
	    $('#mask').show();
	    $('#loadingImg').show();
	}


	function LoadingWithMaskOff() {
		    $('#mask').hide();
		    $('#loadingImg').remove();
	}

	// 인증번호 발송요청 
	$('#service_join_wrap #sendotp').on('click', function(e){
		
		e.stopPropagation();
		e.preventDefault();
		let classList = $('#sendotp').attr('class');
		console.log(classList);
		if (classList == 'btn-success') {
			// =================================================== 
			var telcoSelected = $('#spcode').val().length > 0;
			if (!telcoSelected) {
			    alert('통신사를 선택해주세요.');
			    return;
			}
			
			var phoneNumber = $.trim($('#phoneNumber').val());
			var phoneValid1 = /^010/.test(phoneNumber);
			var phoneValid2 = /^010[1-9]/.test(phoneNumber);
			var phoneValid3 = /^010\d{8}/.test(phoneNumber);
			if (!phoneValid1) {
				alert('전화번호는 010으로 시작해야 합니다.');
			    $('#phoneNumber').focus();
			    return;
			}
			if (!phoneValid2) {
			    //$('#phoneNumberFeedback').show();
				alert('전화번호는 숫자로 입력하여 주세요.');
			    $('#phoneNumber').focus();
			    return;
			}
			if (!phoneValid3) {
				alert('전화번호를 다시 한번 확인해 주세요.');
			    $('#phoneNumber').focus();
			    return;
			}
			
			var formData = {
			    spcode: $('#spcode').val(),
			    mobileno: phoneNumber,
				offercode: $('#offercode').val()
			};
			
			$.ajax({
	            type: 'POST',
	            url: '/api/v1.0/cancel/mobiletown/sendsms',
	            data: JSON.stringify(formData),
	            contentType: 'application/json',
				caches: false, 
				beforeSend: function () {
				        LoadingWithMask(); // 요청 전 로딩 표시
				},
	            success: function (response) {
					LoadingWithMaskOff(); 
					var data = JSON.parse(response);
	                if (data.code === 200) {
	                    alert('인증번호가 발송되었으니, 3분안에 인증번호를 입력하여 주세요.');
	                    $('#steps').val("SENDOTP");
						$('#phoneNumber').prop('readonly', true);
						$('#phoneNumber').css('background-color', '#f0f7fa');
					} else {
						if (data.code === 901) {
							alert('휴대폰약속번호 서비스에 가입이 된 전화번호입니다.[901]');
						} else if (data.code === 923) {
							alert('휴대폰약속번호 서비스에 가입이 불가한 전화번호입니다.[923]');
						} else if (data.code === 3003) {
							alert('조금전에 인증번호를 요청하셨네요. 잠시 후에 다시 시도해주세요.[3003]');
							$('#checkotp').val('');
							$('#checkotp').addClass('disable_btn');
		                } else {
		                    alert(data.msg);
		                }
						$('#sendotp').removeClass('disable_btn');
					}
	            },
				error: function (xhr, status, error) {
				    LoadingWithMaskOff(); // 에러 발생 시 로딩 해제
				    alert('인증번호 발송이 원활하지 않아요. 잠시후 다시 해주세요.[9F5]');
				},
	            complete: function () {
	                //hideLoading();
					//LoadingWithMaskOff();
	            }
	        });
			
			// =================================================== 
			
			$('#sendotp').addClass('disable_btn');
			$('#checkotp').removeClass('disable_btn');
		} else {
			//alert('전화번호를 먼저 입력하세요.');
			//$('#phoneNumber').focus();
		}
	});

	// 인증번호 검증요청  
	$('#service_join_wrap #checkotp').on('click', function(e){
		e.stopPropagation();
		e.preventDefault();
		let classList = $('#checkotp').attr('class');
		console.log(classList);
		if (classList == 'btn-success') {
			// =================================================== 
			var phoneNumber = $.trim($('#phoneNumber').val());
			var otpNumber = $.trim($('#otpNumber').val());

			var formData = {
			    spcode: $('#spcode').val(),
			    mobileno: phoneNumber,
				offercode: $('#offercode').val(),
				rnumber: otpNumber
			};

			$.ajax({
			    type: 'POST',
			    url: '/api/v1.0/cancel/mobiletown/checkotp',
			    data: JSON.stringify(formData),
			    contentType: 'application/json',
				caches: false, 
				beforeSend: function () {
				        LoadingWithMask(); // 요청 전 로딩 표시
				},
			    success: function (response) {
					LoadingWithMaskOff(); 
					var data = JSON.parse(response);
			        if (data.code === 200) {
			            let isCancel = confirm('인증번호 검증이 되었어요. 해지신청 버튼을 누르면 해지할 수 있어요.');
						if (!isCancel) {
							alert("해지신청을 취소하였어요.");
							closeWindows();
						}
						$('#steps').val("CONFIRMOTP");
			            $('#checkotp').addClass("disable_btn");
						$('#checkcode').val(data.checkcode);
					} else {
						if (data.code === 901) {
							alert('휴대폰약속번호 서비스에 가입이 된 전화번호입니다.[901]');
						} else if (data.code === 912) {
							alert('통신사에 가입되지 않은 전화번호입니다.[912]');
						} else if (data.code === 923) {
							alert('휴대폰약속번호 서비스에 가입이 불가한 전화번호입니다.[923]');
						} else if (data.code >= 3102 && data.code <= 3106) {
							alert(data.msg);
							$('#phoneNumber').prop('readonly', false);
							$('#phoneNumber').css('background-color', '#FFFFFF');
							
							$('#checkotp').addClass("disable_btn");
							$('#sendotp').removeClass('disable_btn');
							$('#otpNumber').val('');
							
				        } else {
				            alert(data.msg);
				        }
						$('#checkotp').val('');
					}
			    },
				error: function (xhr, status, error) {
				    LoadingWithMaskOff(); // 에러 발생 시 로딩 해제
				    alert('인증번호 검증이 원활하지 않아요. 잠시후 다시 해주세요.[9F5]');
				},
			    complete: function () {
			        //hideLoading();
					//LoadingWithMaskOff();
			    }
			});
		    // =================================================== 
		}

	});
	
	// 통신사 선택 
	$('#service_join_wrap .service_j_btn').on('click', function(e){
		e.stopPropagation();
		e.preventDefault();
		
		$('#service_join_wrap .service_j_btn').removeClass('selected');
		$(this).addClass('selected');
		
		$('#spcode').val($(this).attr('id'));
		console.log($('#spcode').val());
   });
   
	// 취소처리 
	$('.cancel_btn').on("click", function(){
		// 상태체크
		let steps = $('#steps').val();
		if (steps != "CONFIRMOTP") {
			alert("인증번호를 확인 후에 해지신청을 하셔야해요.");
			return;
		}
		
		// 입력데이터 검증
		var telcoSelected = $('#spcode').val().length > 0;
		var phoneNumber = $.trim($('#phoneNumber').val());
		var phoneValid1 = /^010/.test(phoneNumber);
		var phoneValid2 = /^010[1-9]/.test(phoneNumber);
		var phoneValid3 = /^010\d{8}/.test(phoneNumber);
		var cancelAgree = $('#cancelAgree').is(':checked');
		
		if (!telcoSelected) {
            alert('통신사를 선택해주세요.');
            return;
        }
        
		if (!phoneValid1) {
			alert('전화번호는 010으로 시작해야 합니다.');
            $('#phoneNumber').focus();
            return;
        }
		
		if (!phoneValid2) {
		    //$('#phoneNumberFeedback').show();
			alert('전화번호는 숫자로 입력하여 주세요.');
		    $('#phoneNumber').focus();
		    return;
		}
		
		if (!phoneValid3) {
			alert('전화번호를 다시 한번 확인해 주세요.');
		    $('#phoneNumber').focus();
		    return;
		}

        if (!cancelAgree) {
            alert('서비스 취소확인에 동의하셔야 합니다.');
            return;
        }
		
		var formData = {
		    spcode: $('#spcode').val(),
		    mobileno: phoneNumber,
			offercode: $('#offercode').val()
		};

        $.ajax({
            url: '/api/v1.0/cancel',
            method: 'POST',
            contentType: 'application/json',
			caches: false, 
            data: JSON.stringify(formData),
			beforeSend: function () {
			        LoadingWithMask(); // 요청 전 로딩 표시
			},
            success: function(response) {
				var data = JSON.parse(response);
				LoadingWithMaskOff();
                if (data.code === 200) {
                    alert('휴대폰약속번호 서비스 해지 요청이 정상 처리되었습니다.');
                    closeWindows();
                } else if (data.code === 400 || data.code === 912) {
					alert('휴대폰약속번호 서비스의 가입 내역을 찾을 수 없습니다.\n확인 후 다시 입력바랍니다. ');
				} else {
                    alert('휴대폰약속번호 서비스 해지 요청이 접수되지 못했습니다.\n잠시 후 다시 한번 해주시기 바랍니다.['+data.code+']');
                }
            },
			error: function (xhr, status, error) {
			    LoadingWithMaskOff(); // 에러 발생 시 로딩 해제
			    alert('휴대폰약속번호 해지처리가 원활하지 않네요. 잠시 후 다시 해주세요.[9F5]');
			},
			complete: function () {
			    //hideLoading();
				//LoadingWithMaskOff();
			}
        });
	});

});
