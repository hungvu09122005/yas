pipeline {
    agent any

    stages {
        // --- 1. CI cho MEDIA ---
        stage('Media CI') {
            when {
                // ĐÂY LÀ ĐIỂM CHỐT: Chỉ chạy stage này nếu commit bao gồm thay đổi trong thư mục /media
                changeset "media/**" 
            }
            steps {
                dir('media') { // Chuyển context hoạt động vào thư mục media
                    echo '🔥 Đã nhận diện thay đổi trong /media. Đang chạy CI cho Media Service...'
                    // Thực tế sau này bạn sẽ thay bằng:
                    // sh 'mvn clean test' hoặc './gradlew test'
                }
            }
        }

        // --- 2. CI cho PRODUCT ---
        stage('Product CI') {
            when {
                changeset "product/**" 
            }
            steps {
                dir('product') {
                    echo '🔥 Đã nhận diện thay đổi trong /product. Đang chạy CI cho Product Service...'
                }
            }
        }

        // --- 3. CI cho CART ---
        stage('Cart CI') {
            when {
                changeset "cart/**" 
            }
            steps {
                dir('cart') {
                    echo '🔥 Đã nhận diện thay đổi trong /cart. Đang chạy CI cho Cart Service...'
                }
            }
        }
        
        // Cần cho service nào, bạn copy thêm stage cho service đó tương tự
    }
}
