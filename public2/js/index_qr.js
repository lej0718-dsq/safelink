// ===== 요소 가져오기 =====
const popupOverlay = document.getElementById('popupOverlay');
const closeBtn = document.getElementById('closeBtn');
const confirmBtn = document.getElementById('confirmBtn');
const qrCodeArea = document.getElementById('qrCodeArea');
const qrCodeDiv = document.getElementById('qrCode');
const qrInfoText = document.getElementById('qrInfoText');
const qrDownloadBtn = document.getElementById('qrDownloadBtn');
const successPopup = document.getElementById('successPopup');
const phoneNumberInput = document.getElementById('phoneNumber');
const copyCodeArea = document.getElementById('copyCodeArea');
const copyText = document.getElementById('copyText');
const copyTextBtn = document.getElementById('copyTextBtn');
let qrType = 'copy';
let amcNumber = '';


// ===== 팝업 닫기 =====
closeBtn.addEventListener('click', () => {
    popupOverlay.style.display = 'none';
    resetPopup();
});

// 오버레이 클릭 시 팝업 닫기
popupOverlay.addEventListener('click', (event) => {
    // 실제 배경(overlay) 자체를 클릭했을 경우에만 닫기
    if (event.target === popupOverlay) {
        popupOverlay.style.display = 'none';
        resetPopup();
    }
});

// ===== 확인 버튼 클릭 시 (예: 전화번호 검증 후 QR 영역 노출 준비) =====
confirmBtn.addEventListener('click', () => {
    const phoneNumber = phoneNumberInput.value.trim();
    if (!phoneNumber) {
        alert('전화번호를 입력해주세요.');
        return;
    }
    const phoneValid1 = /^010/.test(phoneNumber);
    const phoneValid2 = /^010[1-9]/.test(phoneNumber);
    const phoneValid3 = /^010\d{8}/.test(phoneNumber);
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

    // 실제 로직에서는 서버 검증, 혹은 약속번호 생성 로직 등이 들어갈 수 있음
    const formData = new FormData();
    formData.append('phoneNumber', phoneNumber);
    $.ajax({
        type: 'POST',
        url: '/api/v1.0/getdata',
        data: JSON.stringify({phoneNumber : phoneNumber}),
        contentType: 'application/json',
        caches: false,
        beforeSend: function () {
            //LoadingWithMask(); // 요청 전 로딩 표시
        },
        success: function (response) {
            //LoadingWithMaskOff();
            var data = JSON.parse(response);
            closeBtn.style.display='block';
            if (data.code === 200) {
                createQrcode(data.msg);
            } else {
                alert(data.msg);
            }
        },
        error: function (xhr, status, error) {
            LoadingWithMaskOff(); // 에러 발생 시 로딩 해제
            alert('잠시 후 다시 해주세요.');
        },
        complete: function () {
            //hideLoading();
            //LoadingWithMaskOff();
        }
    });
});

function createQrcode(serviceNumber) {
    amcNumber = '15555551,' + serviceNumber + '#';
    if(qrType === 'copy') {
        copyCodeArea.style.display = 'block';
        copyText.textContent= `${amcNumber}`;
        copyTextBtn.setAttribute("data-clipboard-text", amcNumber);
    } else {
        // 이미 QR 코드가 있다면 초기화
        qrCodeDiv.innerHTML = '';
        // QR 코드 생성 (qrcode.js 라이브러리 사용)
        const qr = new QRCode(qrCodeDiv, {
            text: 'tel:'+amcNumber,  // 실제 원하는 텍스트나 URL
            width: 120,
            height: 120
        });
        // 예시로 QR 하단 텍스트 설정
        qrInfoText.textContent = `${amcNumber}`;
        // QR 영역 및 다운로드 버튼 노출
        qrCodeArea.style.display = 'block';
        qrDownloadBtn.style.display = 'grid';
    }
}

// ===== QR 다운로드 버튼 클릭 =====
qrDownloadBtn.addEventListener('click', () => {
    // canvas 태그를 이미지로 변환
    const canvas = qrCodeDiv.querySelector('canvas');
    if (!canvas) return;

    const dataUrl = canvas.toDataURL('image/png');
    const link = document.createElement('a');
    link.href = dataUrl;
    link.download = 'qr_code.png';
    link.click();

    // 다운로드 완료 메시지 표시
    //showSuccessPopup();
});

function showSuccessPopup() {
    let text = '';
    if(qrType === 'copy') {
        text = '약속번호가 복사되었습니다';
    } else {
        text = 'QR 다운로드가 완료되었습니다'
    }
    successPopup.innerText=text;
    // 팝업 보이기
    //successPopup.style.display = 'block';
    successPopup.classList.add('show')

    // 일정 시간 후 자동으로 숨기기 (예: 2초 후)
    setTimeout(() => {
        successPopup.classList.remove('show');
        //successPopup.style.display = 'none';
    }, 2000);
}

// ===== 팝업 리셋 함수 =====
function resetPopup() {
    phoneNumberInput.value = '';
    qrCodeArea.style.display = 'none';
    qrDownloadBtn.style.display = 'none';
    qrCodeDiv.innerHTML = '';
    qrInfoText.textContent = '';
    closeBtn.style.display = 'none';
    copyCodeArea.style.display = 'none';
}