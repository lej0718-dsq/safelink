function closeWindows() {
	if (confirm("창을 닫을까요?")) {
		window.open('about:blank', '_self').close();
	}
}

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
				url: '/api/v1.0/subscribe/verifiedCheck',
				data: JSON.stringify(formData),
				contentType: 'application/json',
				beforeSend: function () {
					LoadingWithMask();
				},
				success: function (precheckRes) {
					LoadingWithMaskOff();
					var precheck = typeof precheckRes === "string" ? JSON.parse(precheckRes) : precheckRes;

					if (precheck.captchaRequired) {
						// CAPTCHA 필요시: CAPTCHA 입력 모달 띄우기
						showCaptchaModal(precheck.captchaQuestion, formData);
					} else {
						// CAPTCHA 불필요: 바로 인증번호 발송
						sendOtp(formData);
					}
				},
				error: function () {
					LoadingWithMaskOff();
					alert('서버 오류. 잠시 후 다시 시도해주세요.');
				}
			});
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
				url: '/api/v1.0/subscribe/mobiletown/checkotp',
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
						alert('인증번호 검증이 되었어요. 약관동의 후 가입버튼을 눌러주세요.');
						$('#steps').val("CONFIRMOTP");
						$('#checkotp').addClass("disable_btn");
						$('#checkcode').val(data.checkcode);
						// PG결제 설정
						$('#MC_No').val(phoneNumber);
						$('input[name=Tradeid]').val(phoneNumber+new Date().getTime());
						$('#MC_DEFAULTCOMMID').val($('#spcode').val());
						$('#MC_FIXCOMMID').val($('#spcode').val());
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
		if($(this).attr('id') !== "SKT") {
			$(".pay_wrap").css("display", "flex");
		} else {
			$(".pay_wrap").css("display", "none");
		}
	});


	// popup에서 allcheck 기능
	// allcheck 체크박스를 클릭했을 때
	$('#allcheck').on('change', function() {
		var isChecked = $(this).is(':checked');
		// agree1, agree2, agree3 체크박스의 상태를 allcheck의 상태와 동일하게 설정
		$('input[name="agree1"], input[name="agree2"], input[name="agree3"]').prop('checked', isChecked);
	});

	// agree1, agree2, agree3 체크박스를 클릭했을 때
	$('input[name="agree1"], input[name="agree2"], input[name="agree3"]').on('change', function() {
		// 모든 agree 체크박스가 체크된 상태인지 확인
		var allChecked = $('input[name="agree1"]').is(':checked') &&
			$('input[name="agree2"]').is(':checked') &&
			$('input[name="agree3"]').is(':checked');
		// allcheck 체크박스의 상태를 모든 agree 체크박스의 상태에 따라 설정
		$('#allcheck').prop('checked', allChecked);
	});

	// 가입처리
	$('.button').on("click", function(){
		// 상태체크
		let steps = $('#steps').val();

		if (steps != "CONFIRMOTP") {
			alert("인증번호를 확인 후에 가입을 하셔야해요.");
			return;
		}

		// 입력데이터 검증
		var telcoSelected = $('#spcode').val().length > 0;
		var phoneNumber = $.trim($('#phoneNumber').val());
		var phoneValid1 = /^010/.test(phoneNumber);
		var phoneValid2 = /^010[1-9]/.test(phoneNumber);
		var phoneValid3 = /^010\d{8}/.test(phoneNumber);
		var allAgreed = $('#agree1').is(':checked') && $('#agree2').is(':checked') && $('#agree3').is(':checked');

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

		if (!allAgreed) {
			alert('서비스약관, 개인정보수집동의, 제3자정보제공동의는 꼭 필요한 내용입니다.');
			if (!$('#agree1').is(':checked')) {
				$('#agree1').focus();
			} else if (!$('#agree2').is(':checked')) {
				$('#agree2').focus();
			} else if (!$('#agree3').is(':checked')) {
				$('#agree3').focus();
			}
			return;
		}

		$('input[name=MSTR]').val("offercode="+$('#offercode').val()+"|agree1="+$('#agree1').is(':checked')+"|agree2="+$('#agree2').is(':checked')+"|agree3="+$('#agree3').is(':checked') + "|spcode="+$('#spcode').val()+"|bz_tracking_id="+sessionStorage.getItem("bz_tracking_id")+"|bz_checkcode="+$('#checkcode').val()+"|ohvalue="+sessionStorage.getItem("ohvalue")+"|m_id="+sessionStorage.getItem("m_id"));

		var formData = {
			spcode: $('#spcode').val(),
			mobileno: phoneNumber,
			offercode: $('#offercode').val(),
			checkcode: $('#checkcode').val(),
			agree1: $('#agree1').is(':checked'),
			agree2: $('#agree2').is(':checked'),
			agree3: $('#agree3').is(':checked')
		};
		// PG결제 분기 처리 2025.02.10 양세용
		if($('#spcode').val() !== 'SKT') {
			if($('#payGbn').val() === 'phone') {
				$.ajax({
					type: 'POST',
					url: '/api/v1.0/mobilians/precheck',
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
							payRequest();
						} else if (data.code === 901) {
							alert('휴대폰약속번호 서비스에 가입이 된 전화번호입니다.[901]');
						} else if (data.code === 912) {
							alert('통신사에 가입되지 않은 전화번호입니다.[912]');
						} else if (data.code === 923) {
							alert('휴대폰약속번호 서비스에 가입이 불가한 전화번호입니다.[923]');
						} else if(data.code === 902){
							alert('입력하신 전화번호는 해지관련 전산처리 중으로 내일 가입이 가능합니다.[902]');
						} else {

						}
					},
					error: function (xhr, status, error) {
						LoadingWithMaskOff(); // 에러 발생 시 로딩 해제
						alert('휴대폰약속번호 서비스 가입이 원활하지 않네요. 잠시 후 다시 해주세요.[9F5]');
					},
					complete: function () {
						//hideLoading();
						//LoadingWithMaskOff();
					}
				});
			} else {
				//payRequestCN();
			}
		} else {
			$.ajax({
				type: 'POST',
				url: '/api/v1.0/subscribe',
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
						//var ohc = setEventEntry(phoneNumber); // 여기서 closewindow를 함
						
						// sessionStorage에 휴대폰 번호 저장 (키-값 쌍으로)
						sessionStorage.setItem('allonePhoneNumber', phoneNumber);
						console.log('sessionStorage에 휴대폰 번호 저장:', phoneNumber);
						
						// 완료 페이지로 리다이렉트
						window.location.href = "https://linksafe.kr/subscribe_allone_done.html";
						//$('#subscriptionModal').modal('hide');
					} else if (data.code === 901) {
						alert('휴대폰약속번호 서비스에 가입이 된 전화번호입니다.[901]');
					} else if (data.code === 912) {
						alert('통신사에 가입되지 않은 전화번호입니다.[912]');
					} else if (data.code === 923) {
						alert('휴대폰약속번호 서비스에 가입이 불가한 전화번호입니다.[923]');
					} else {
						alert(data.msg);
					}
				},
				error: function (xhr, status, error) {
					LoadingWithMaskOff(); // 에러 발생 시 로딩 해제
					alert('휴대폰약속번호 서비스 가입이 원활하지 않네요. 잠시 후 다시 해주세요.[9F5]');
				},
				complete: function () {
					//hideLoading();
					//LoadingWithMaskOff();
				}
			});
		}
		// ===============================

	});

	//캡차 입력 UI 및 처리 함수
	function showCaptchaModal(question, formData) {
		$('#captchaModal .captcha-question').text(question);
		$('#captchaModal').show();
		$('#captchaInput').val('').focus();
		
		// 확인 버튼 클릭 이벤트
		$('#captchaModal #captchaSubmit').off('click').on('click', function() {
			var captchaAnswer = $('#captchaModal #captchaInput').val().trim();
			if (!captchaAnswer) {
				alert('캡차를 입력해주세요.');
				return;
			}
			
			// 캡차 검증 요청
			$.ajax({
				type: 'POST',
				url: '/api/v1.0/subscribe/verifyCaptcha',
				data: 'captcha=' + encodeURIComponent(captchaAnswer),
				contentType: 'application/x-www-form-urlencoded',
				success: function(response) {
					var result = typeof response === "string" ? JSON.parse(response) : response;
					if (result.success) {
						$('#captchaModal').hide();
						sendOtp(formData);
					} else {
						alert('캡차가 일치하지 않습니다. 다시 입력해주세요.');
						$('#captchaInput').val('').focus();
					}
				},
				error: function() {
					alert('캡차 검증 중 오류가 발생했습니다.');
				}
			});
		});
		
		// 취소 버튼 클릭 이벤트
		$('#captchaModal #captchaCancel').off('click').on('click', function() {
			$('#captchaModal').hide();
		});
		
		// Enter 키 처리
		$('#captchaInput').off('keypress').on('keypress', function(e) {
			if (e.which === 13) {
				$('#captchaSubmit').click();
			}
		});
	}

	// 실제 인증번호 발송 함수
	function sendOtp(formData){
		$.ajax({
			type: 'POST',
			url: '/api/v1.0/subscribe/mobiletown/sendsms',
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
					$('#sendotp').addClass('disable_btn');
					$('#checkotp').removeClass('disable_btn');
				} else {
					if (data.code === 901) {
						alert('휴대폰약속번호 서비스에 가입이 된 전화번호입니다.[901]');
					} else if (data.code === 912) {
						alert('통신사에 가입되지 않은 전화번호입니다.[912]');
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
				$('#sendotp').removeClass('disable_btn');
			},
			complete: function () {
				//hideLoading();
				//LoadingWithMaskOff();
			}
		});
	}

});
