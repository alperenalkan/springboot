# 📈 Bitcoin Al/Sat Sinyali Üreten Web Uygulaması

Bu proje, Bitcoin'in geçmiş fiyat verilerini analiz ederek teknik indikatörlere dayalı alım-satım sinyalleri üreten bir Spring Boot web uygulamasıdır.

## 🎯 Özellikler

- **Gerçek Zamanlı Veri**: CoinGecko API'den Bitcoin fiyat verilerini otomatik çekme
- **Teknik Analiz**: RSI, MACD, SMA, EMA indikatörleri ile analiz
- **Sinyal Üretimi**: "Al", "Sat", "Bekle" sinyalleri
- **Çoklu Zaman Dilimi**: 1 Saat, 4 Saat, 1 Gün periyotları
- **Modern Web Arayüzü**: Responsive ve kullanıcı dostu frontend
- **REST API**: JSON tabanlı API endpoints
- **Otomatik Veri Güncelleme**: 5 dakikada bir otomatik veri çekme

## 🏗️ Teknoloji Stack

### Backend
- **Spring Boot 3.2.0** - Ana framework
- **Spring Data JPA** - Veritabanı işlemleri
- **PostgreSQL** - Ana veritabanı
- **H2 Database** - Test veritabanı
- **WebFlux** - HTTP client için
- **Jackson** - JSON işlemleri

### Frontend
- **HTML5/CSS3** - Modern ve responsive tasarım
- **JavaScript (ES6+)** - Dinamik işlevsellik
- **Chart.js** - Grafik gösterimi
- **Bootstrap-like CSS** - Özel tasarım sistemi

## 📊 Teknik İndikatörler

### RSI (Relative Strength Index)
- **Periyot**: 14 (varsayılan)
- **Aşırı Satım**: < 30
- **Aşırı Alım**: > 70

### MACD (Moving Average Convergence Divergence)
- **Hızlı EMA**: 12 periyot
- **Yavaş EMA**: 26 periyot
- **Sinyal**: 9 periyot

### Moving Averages
- **SMA 20**: 20 periyot basit hareketli ortalama
- **EMA 12**: 12 periyot üstel hareketli ortalama

## 🚀 Kurulum

### Gereksinimler
- Java 17+
- Maven 3.6+
- PostgreSQL 12+

### 1. Veritabanı Kurulumu
```sql
CREATE DATABASE bitcoin_signals;
CREATE USER bitcoin_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE bitcoin_signals TO bitcoin_user;
```

### 2. Konfigürasyon
`src/main/resources/application.properties` dosyasını düzenleyin:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bitcoin_signals
spring.datasource.username=bitcoin_user
spring.datasource.password=your_password
```

### 3. Uygulamayı Çalıştırma
```bash
# Projeyi derle
mvn clean compile

# Uygulamayı başlat
mvn spring-boot:run
```

Uygulama `http://localhost:8080` adresinde çalışacaktır.

## 📡 API Endpoints

### Fiyat Verileri
- `GET /api/price/{interval}` - Belirli periyottaki fiyat verileri
- `GET /api/price/{interval}/latest/{limit}` - Son N kayıt
- `GET /api/price/{interval}/latest` - En son fiyat
- `GET /api/price/{interval}/range` - Tarih aralığındaki veriler

### Sinyaller
- `GET /api/signal/{interval}` - Teknik analiz sinyali

### Veri Çekme
- `POST /api/fetch/{interval}` - Manuel veri çekme
- `POST /api/fetch/all` - Tüm interval'lar için veri çekme
- `GET /api/fetch/status` - Veri çekme durumu

### Sağlık Kontrolü
- `GET /api/health` - Uygulama durumu

### Interval Değerleri
- `1h` veya `1hour` - 1 Saat
- `4h` veya `4hours` - 4 Saat
- `1d` veya `1day` - 1 Gün

## 🧪 Testler

```bash
# Tüm testleri çalıştır
mvn test

# Sadece unit testler
mvn test -Dtest=IndicatorServiceTest

# Test coverage
mvn test jacoco:report
```

## 📁 Proje Yapısı

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── Main.java                 # Spring Boot uygulaması
│   │   ├── controller/
│   │   │   ├── PriceController.java  # Fiyat API endpoints
│   │   │   └── FetchController.java  # Veri çekme endpoints
│   │   ├── service/
│   │   │   ├── PriceService.java     # Ana iş mantığı
│   │   │   ├── IndicatorService.java # Teknik indikatörler
│   │   │   └── FetchService.java     # Veri çekme servisi
│   │   ├── repository/
│   │   │   └── PriceRepository.java  # Veritabanı işlemleri
│   │   ├── entity/
│   │   │   └── PriceEntity.java      # Veritabanı modeli
│   │   └── dto/
│   │       ├── PriceDto.java         # Fiyat veri transfer objesi
│   │       └── SignalDto.java        # Sinyal veri transfer objesi
│   └── resources/
│       ├── application.properties    # Konfigürasyon
│       └── static/
│           └── index.html            # Frontend
└── test/
    └── java/org/example/
        └── service/
            └── IndicatorServiceTest.java # Unit testler
```

## 🎨 Frontend Özellikleri

- **Responsive Tasarım**: Mobil ve masaüstü uyumlu
- **Gerçek Zamanlı Grafik**: Chart.js ile interaktif grafik
- **Sinyal Gösterimi**: Renkli ikonlarla sinyal durumu
- **İndikatör Paneli**: RSI, MACD, SMA değerleri
- **Otomatik Yenileme**: Veri güncelleme butonları

## 🔧 Konfigürasyon

### Uygulama Ayarları
```properties
# Server
server.port=8080

# Veritabanı
spring.datasource.url=jdbc:postgresql://localhost:5432/bitcoin_signals
spring.jpa.hibernate.ddl-auto=update

# API
app.coingecko.base-url=https://api.coingecko.com/api/v3
app.coingecko.timeout=10000

# Scheduling
app.scheduling.enabled=true
app.scheduling.price-fetch-interval=300000

# İndikatörler
app.indicators.rsi.period=14
app.indicators.rsi.oversold=30
app.indicators.rsi.overbought=70
```

## 📈 Kullanım Senaryoları

### 1. Günlük Trading
- 1 saatlik ve 4 saatlik periyotları takip edin
- RSI aşırı satım seviyelerinde alım yapın
- MACD kesişimlerini izleyin

### 2. Swing Trading
- 1 günlük periyotları analiz edin
- SMA 20 seviyelerini destek/direnç olarak kullanın
- EMA 12 trend yönünü belirler

### 3. Risk Yönetimi
- Sadece sinyallere güvenmeyin
- Stop-loss kullanın
- Pozisyon büyüklüğünü kontrol edin

## ⚠️ Uyarılar

- Bu uygulama sadece eğitim amaçlıdır
- Gerçek trading için profesyonel danışmanlık alın
- Kripto para yatırımları yüksek risk içerir
- Geçmiş performans gelecek garantisi değildir

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push yapın (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## 📝 Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

## 📞 İletişim

- **Proje**: [GitHub Repository](https://github.com/yourusername/bitcoin-signal-app)
- **Sorular**: Issues bölümünü kullanın

## 🔄 Güncellemeler

### v1.0.0
- İlk sürüm
- Temel teknik analiz indikatörleri
- REST API
- Modern web arayüzü
- PostgreSQL entegrasyonu

---

**Not**: Bu uygulama demo amaçlıdır. Gerçek trading kararları için profesyonel finansal danışmanlık alın. 