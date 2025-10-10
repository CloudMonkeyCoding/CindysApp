<?php
$baseDir = str_replace('\\', '/', realpath(__DIR__) ?: __DIR__);

$scriptPath = '';
if (isset($_SERVER['SCRIPT_FILENAME']) && is_string($_SERVER['SCRIPT_FILENAME'])) {
    $scriptPath = $_SERVER['SCRIPT_FILENAME'];
} else {
    $trace = debug_backtrace(DEBUG_BACKTRACE_IGNORE_ARGS);
    foreach ($trace as $frame) {
        if (!empty($frame['file']) && is_string($frame['file'])) {
            $scriptPath = $frame['file'];
            break;
        }
    }
}

if ($scriptPath === '') {
    $scriptPath = __FILE__;
}

$scriptPath = str_replace('\\', '/', realpath($scriptPath) ?: $scriptPath);
$scriptDir = str_replace('\\', '/', dirname($scriptPath));
$scriptDir = $scriptDir !== '' ? $scriptDir : $baseDir;

$depth = 0;
if (strpos($scriptDir, $baseDir) === 0) {
    $relative = trim(substr($scriptDir, strlen($baseDir)), '/');
    $depth = $relative === '' ? 0 : substr_count($relative, '/') + 1;
} else {
    $baseParts = explode('/', trim($baseDir, '/'));
    $scriptParts = explode('/', trim($scriptDir, '/'));
    $maxCommon = min(count($baseParts), count($scriptParts));
    $common = 0;
    while ($common < $maxCommon && $baseParts[$common] === $scriptParts[$common]) {
        $common++;
    }
    $depth = count($scriptParts) - $common;
}

$rootPrefix = $depth === 0 ? '' : str_repeat('../', $depth);
$userPrefix = $rootPrefix . 'UserSide/';
$imagesBase = $rootPrefix . 'Images/';
$productBase = $userPrefix . 'PRODUCT/';
?>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Cindy's Bakeshop Hagonoy - Fresh Baked Delights</title>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <link rel="stylesheet" href="<?= htmlspecialchars($userPrefix . 'styles.css', ENT_QUOTES) ?>">
    <style>
      * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
      }

      html {
        scroll-behavior: smooth;
      }

      body {
        font-family: 'Poppins', sans-serif;
        background: linear-gradient(135deg, #faf8f5 0%, #f5f3f0 100%);
        color: #8b4513;
        line-height: 1.6;
        overflow-x: hidden;
      }

      .download-btn {
        background: linear-gradient(135deg, #8b4513, #a0522d);
        color: #fff !important;
        padding: 0.7rem 1.5rem;
        border-radius: 25px;
        font-weight: 600;
        transition: all 0.3s ease;
        box-shadow: 0 4px 15px rgba(139, 69, 19, 0.3);
        text-decoration: none;
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
      }

      .download-btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(139, 69, 19, 0.4);
      }

      .hamburger {
        display: none;
        font-size: 1.8rem;
        cursor: pointer;
        color: #8b4513;
        transition: transform 0.3s ease;
      }

      .hamburger:hover {
        transform: scale(1.1);
      }

       /* Hero Section */
       .hero {
         min-height: 100vh;
         display: flex;
         align-items: center;
         justify-content: space-between;
         padding: 8rem 8% 4rem;
         background: linear-gradient(135deg, #faf8f5 0%, #f5f3f0 100%);
         position: relative;
         overflow: hidden;
       }

       .hero::before {
         content: '';
         position: absolute;
         top: -50%;
         right: -20%;
         width: 40%;
         height: 200%;
         background: linear-gradient(45deg, rgba(139, 69, 19, 0.05), rgba(160, 82, 45, 0.02));
         transform: rotate(15deg);
         z-index: 1;
       }

       .hero::after {
         content: '';
         position: absolute;
         bottom: -30%;
         left: -10%;
         width: 30%;
         height: 150%;
         background: linear-gradient(45deg, rgba(139, 69, 19, 0.03), rgba(160, 82, 45, 0.01));
         transform: rotate(-10deg);
         z-index: 1;
       }

      .hero-content {
        flex: 1;
        max-width: 600px;
        z-index: 2;
        position: relative;
      }

       .hero-badge {
         display: inline-block;
         background: linear-gradient(135deg, #8b4513, #a0522d);
         color: white;
         padding: 0.6rem 1.2rem;
         border-radius: 6px;
         font-size: 0.85rem;
         font-weight: 600;
         margin-bottom: 1.5rem;
         animation: pulse 2s infinite;
         box-shadow: 0 4px 15px rgba(139, 69, 19, 0.2);
         border: 1px solid rgba(139, 69, 19, 0.2);
       }

      @keyframes pulse {
        0%, 100% { transform: scale(1); }
        50% { transform: scale(1.05); }
      }

       .hero h1 {
         font-size: 4.5rem;
         font-weight: 900;
         margin-bottom: 2rem;
         color: #8b4513;
         line-height: 1.1;
         animation: slideInLeft 1s ease-out;
         text-shadow: 0 4px 20px rgba(139, 69, 19, 0.2);
       }

      @keyframes slideInLeft {
        from {
          opacity: 0;
          transform: translateX(-50px);
        }
        to {
          opacity: 1;
          transform: translateX(0);
        }
      }

       .hero p {
         font-size: 1.4rem;
         color: #8b4513;
         margin-bottom: 2.5rem;
         line-height: 1.8;
         animation: slideInLeft 1s ease-out 0.2s both;
       }

      .hero-buttons {
        display: flex;
        gap: 1.5rem;
        flex-wrap: wrap;
        animation: slideInLeft 1s ease-out 0.4s both;
      }

       .btn-primary {
         background: linear-gradient(135deg, #8b4513, #a0522d);
         color: #fff;
         border: 2px solid #8b4513;
         padding: 0.8rem 1.5rem;
         border-radius: 8px;
         font-size: 1rem;
         font-weight: 600;
         cursor: pointer;
         transition: all 0.3s ease;
         box-shadow: 0 4px 15px rgba(139, 69, 19, 0.2);
         text-decoration: none;
         display: inline-flex;
         align-items: center;
         gap: 0.5rem;
       }

       .btn-primary:hover {
         background: linear-gradient(135deg, #a0522d, #8b4513);
         transform: translateY(-2px);
         box-shadow: 0 6px 20px rgba(139, 69, 19, 0.3);
         color: #fff;
       }

      .btn-secondary {
        background: transparent;
        color: #8b4513;
        border: 2px solid #8b4513;
        padding: 0.8rem 1.5rem;
        border-radius: 8px;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s ease;
        text-decoration: none;
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
      }

      .btn-secondary:hover {
        background: #8b4513;
        color: #fff;
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(139, 69, 19, 0.3);
      }

      .hero-image {
        flex: 1;
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 2;
        position: relative;
      }

      .hero-image img {
        max-width: 550px;
        width: 100%;
        border-radius: 40px;
        box-shadow: 0 40px 80px rgba(0, 0, 0, 0.4);
        animation: float 6s ease-in-out infinite;
        border: 4px solid rgba(255, 255, 255, 0.3);
        -webkit-backdrop-filter: blur(10px);
        backdrop-filter: blur(10px);
        transition: all 0.3s ease;
      }

      @keyframes float {
        0%, 100% { transform: translateY(0px); }
        50% { transform: translateY(-20px); }
      }


      /* Categories */
      .categories {
        padding: 8rem 8%;
        background: linear-gradient(135deg, #faf8f5 0%, #f5f3f0 100%);
        position: relative;
      }

      .section-header {
        text-align: center;
        margin-bottom: 4rem;
      }

      .section-title {
        font-size: 2.5rem;
        font-weight: 700;
        color: #8b4513;
        margin-bottom: 1rem;
        position: relative;
        display: inline-block;
      }

      .section-title::after {
        content: '';
        position: absolute;
        bottom: -15px;
        left: 50%;
        transform: translateX(-50%);
        width: 80px;
        height: 5px;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        border-radius: 3px;
        box-shadow: 0 4px 15px rgba(139, 69, 19, 0.3);
      }

      .section-subtitle {
        font-size: 1.2rem;
        color: #8b4513;
        max-width: 600px;
        margin: 0 auto;
      }

      .categories-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 2rem;
        margin-top: 3rem;
      }

      .category-card {
        background: linear-gradient(135deg, #faf8f5 0%, #f5f3f0 100%);
        border-radius: 25px;
        padding: 3rem;
        text-align: center;
        box-shadow: 0 20px 40px rgba(139, 69, 19, 0.1);
        transition: all 0.4s ease;
        position: relative;
        overflow: hidden;
        border: 1px solid rgba(139, 69, 19, 0.1);
        -webkit-backdrop-filter: blur(10px);
        backdrop-filter: blur(10px);
      }

      .category-card::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 6px;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        transform: scaleX(0);
        transition: transform 0.4s ease;
        border-radius: 25px 25px 0 0;
      }

      .category-card:hover::before {
        transform: scaleX(1);
      }

      .category-card:hover {
        transform: translateY(-15px) scale(1.02);
        box-shadow: 0 30px 60px rgba(139, 69, 19, 0.2);
      }

      .category-icon {
        width: 100px;
        height: 100px;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 2rem;
        font-size: 2.5rem;
        color: white;
        transition: all 0.4s ease;
        box-shadow: 0 15px 40px rgba(139, 69, 19, 0.4);
        border: 3px solid rgba(250, 248, 245, 0.2);
      }

      .category-card:hover .category-icon {
        transform: scale(1.2) rotate(8deg);
        box-shadow: 0 20px 50px rgba(139, 69, 19, 0.5);
      }

      .category-card h3 {
        font-size: 1.5rem;
        font-weight: 600;
        color: #8b4513;
        margin-bottom: 1rem;
      }

      .category-card p {
        color: #666;
        font-size: 1rem;
        line-height: 1.6;
      }

      /* Products */
      .products {
        padding: 8rem 8%;
        background: linear-gradient(135deg, #faf8f5 0%, #f5f3f0 100%);
        position: relative;
        overflow: hidden;
      }

      .products::before {
        content: '';
        position: absolute;
        top: -50%;
        right: -20%;
        width: 40%;
        height: 200%;
        background: linear-gradient(45deg, rgba(139, 69, 19, 0.03), rgba(160, 82, 45, 0.01));
        transform: rotate(15deg);
        z-index: 1;
      }

      .products::after {
        content: '';
        position: absolute;
        bottom: -30%;
        left: -10%;
        width: 30%;
        height: 150%;
        background: linear-gradient(45deg, rgba(139, 69, 19, 0.02), rgba(160, 82, 45, 0.005));
        transform: rotate(-10deg);
        z-index: 1;
      }

      .products-carousel {
        position: relative;
        margin-top: 4rem;
        z-index: 2;
      }

      .carousel-track-wrapper {
        overflow: hidden;
        width: 100%;
        padding: 0 1.5rem;
      }

      .products-track {
        display: flex;
        gap: 2.5rem;
        align-items: stretch;
        transition: transform 0.6s ease;
      }

      .products-track .product-card {
        flex: 0 0 100%;
        max-width: 100%;
      }

      .carousel-control {
        position: absolute;
        top: 50%;
        transform: translateY(-50%);
        width: 52px;
        height: 52px;
        border-radius: 50%;
        border: none;
        background: rgba(139, 69, 19, 0.92);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        box-shadow: 0 20px 45px rgba(139, 69, 19, 0.25);
        transition: transform 0.3s ease, box-shadow 0.3s ease, background 0.3s ease;
        z-index: 3;
      }

      .carousel-control:hover,
      .carousel-control:focus-visible {
        background: #a0522d;
        transform: translateY(-50%) scale(1.05);
        box-shadow: 0 25px 55px rgba(139, 69, 19, 0.35), 0 0 0 3px rgba(255, 255, 255, 0.65);
        outline: none;
      }

      .carousel-control.prev {
        left: 0.5rem;
      }

      .carousel-control.next {
        right: 0.5rem;
      }

      .product-card {
        background: linear-gradient(135deg, #faf8f5 0%, #f5f3f0 100%);
        border-radius: 30px;
        overflow: hidden;
        box-shadow: 0 20px 50px rgba(139, 69, 19, 0.1);
        transition: all 0.5s ease;
        position: relative;
        border: 3px solid rgba(139, 69, 19, 0.08);
        -webkit-backdrop-filter: blur(10px);
        backdrop-filter: blur(10px);
      }

      .product-card:hover {
        transform: translateY(-20px) scale(1.05);
        box-shadow: 0 30px 70px rgba(139, 69, 19, 0.25);
        border-color: rgba(139, 69, 19, 0.2);
      }

      .product-card::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 4px;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        transform: scaleX(0);
        transition: transform 0.5s ease;
        border-radius: 30px 30px 0 0;
        z-index: 3;
      }

      .product-card:hover::before {
        transform: scaleX(1);
      }

      .product-image {
        position: relative;
        overflow: hidden;
        height: 220px;
        border-radius: 25px 25px 0 0;
      }

      .product-image img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.5s ease;
        border-radius: 25px 25px 0 0;
      }

      .product-card:hover .product-image img {
        transform: scale(1.15);
      }

      .product-image::after {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(135deg, rgba(139, 69, 19, 0.1), rgba(160, 82, 45, 0.05));
        opacity: 0;
        transition: opacity 0.5s ease;
        border-radius: 25px 25px 0 0;
      }

      .product-card:hover .product-image::after {
        opacity: 1;
      }

      .product-badge {
        position: absolute;
        top: 1.2rem;
        right: 1.2rem;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        color: white;
        padding: 0.6rem 1.2rem;
        border-radius: 25px;
        font-size: 0.75rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        box-shadow: 0 8px 20px rgba(139, 69, 19, 0.3);
        border: 2px solid rgba(255, 255, 255, 0.2);
        -webkit-backdrop-filter: blur(10px);
        backdrop-filter: blur(10px);
        z-index: 4;
        transition: all 0.3s ease;
      }

      .product-card:hover .product-badge {
        transform: scale(1.1);
        box-shadow: 0 12px 25px rgba(139, 69, 19, 0.4);
      }

      .product-info {
        padding: 2rem 1.8rem;
        background: linear-gradient(135deg, rgba(250, 248, 245, 0.8), rgba(245, 243, 240, 0.9));
        -webkit-backdrop-filter: blur(10px);
        backdrop-filter: blur(10px);
      }

      .product-info h4 {
        font-size: 1.4rem;
        font-weight: 700;
        color: #8b4513;
        margin-bottom: 0.8rem;
        line-height: 1.3;
        text-align: center;
      }

      .product-info p {
        color: #8b4513;
        font-size: 0.95rem;
        margin-bottom: 1.5rem;
        line-height: 1.6;
        min-height: 2.5rem;
        text-align: center;
        opacity: 0.9;
      }

      .product-price {
        font-size: 1.8rem;
        font-weight: 800;
        color: #8b4513;
        margin-bottom: 0;
        text-align: center;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        position: relative;
        padding: 0.5rem 0;
      }

      .product-price::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 60%;
        height: 2px;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        border-radius: 1px;
        opacity: 0;
        transition: opacity 0.3s ease;
      }

      .product-card:hover .product-price::after {
        opacity: 1;
      }


      /* About Us */
      .about {
        padding: 6rem 8%;
        background: #faf8f5;
        display: flex;
        align-items: center;
        gap: 4rem;
      }

      .about-content {
        flex: 1;
      }

      .about-image {
        flex: 1;
        text-align: center;
      }

      .about-image img {
        max-width: 100%;
        border-radius: 20px;
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
      }

      .about h2 {
        font-size: 2.5rem;
        font-weight: 700;
        color: #8b4513;
        margin-bottom: 1.5rem;
        position: relative;
      }

      .about h2::after {
        content: '';
        position: absolute;
        bottom: -10px;
        left: 0;
        width: 60px;
        height: 4px;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        border-radius: 2px;
      }

      .about p {
        font-size: 1.1rem;
        color: #8b4513;
        line-height: 1.8;
        margin-bottom: 2rem;
      }

      .about-features {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1.5rem;
        margin-top: 2rem;
      }

      .feature {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1rem;
        background: #faf8f5;
        border-radius: 10px;
        transition: all 0.3s ease;
      }

      .feature:hover {
        background: #f5f3f0;
        transform: translateX(5px);
      }

      .feature-icon {
        width: 40px;
        height: 40px;
        background: linear-gradient(135deg, #8b4513, #a0522d);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-size: 1.2rem;
      }

       /* Visit Us */
       .visit {
         padding: 6rem 8%;
         background: linear-gradient(135deg, #faf8f5 0%, #f5f3f0 100%);
         text-align: center;
       }

      .visit-info {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 2rem;
        margin: 3rem 0;
      }


       .info-card h3 {
         font-size: 1.3rem;
         font-weight: 600;
         color: #8b4513;
         margin-bottom: 1rem;
         display: flex;
         align-items: center;
         justify-content: center;
         gap: 0.5rem;
       }

      .info-card p {
        color: #8b4513;
        font-size: 1rem;
        line-height: 1.6;
      }

      .info-card {
        background: #faf8f5;
        padding: 2rem;
        border-radius: 20px;
        box-shadow: 0 10px 30px rgba(139, 69, 19, 0.1);
        transition: all 0.3s ease;
        border: 1px solid rgba(139, 69, 19, 0.1);
      }

      .map-container {
        margin-top: 3rem;
        border-radius: 20px;
        overflow: hidden;
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
      }

      .map-container iframe {
        width: 100%;
        height: 400px;
        border: none;
      }

      /* CTA */
      .cta {
        background: linear-gradient(135deg, #8b4513, #a0522d);
        color: #faf8f5;
        padding: 6rem 8%;
        text-align: center;
        position: relative;
        overflow: hidden;
      }

      .cta::before {
        content: '';
        position: absolute;
        top: -50%;
        left: -20%;
        width: 40%;
        height: 200%;
        background: rgba(255, 255, 255, 0.1);
        transform: rotate(-15deg);
      }

      .cta-content {
        position: relative;
        z-index: 2;
      }

      .cta h2 {
        font-size: 2.5rem;
        font-weight: 700;
        margin-bottom: 1rem;
      }

      .cta p {
        font-size: 1.2rem;
        margin-bottom: 2.5rem;
        opacity: 0.9;
      }

      .cta .download-btn {
        background: #faf8f5;
        color: #8b4513 !important;
        font-size: 1.1rem;
        padding: 1rem 2rem;
      }

      /* Footer */
      footer {
        background: #8b4513;
        color: #faf8f5;
        padding: 3rem 8% 2rem;
        text-align: center;
      }

      .footer-content {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 2rem;
        margin-bottom: 2rem;
      }

      .footer-section h3 {
        font-size: 1.3rem;
        font-weight: 600;
        margin-bottom: 1rem;
        color: #faf8f5;
      }

      .footer-section p,
      .footer-section a {
        color: #f5f3f0;
        text-decoration: none;
        line-height: 1.8;
        transition: color 0.3s ease;
      }

      .footer-section a:hover {
        color: #faf8f5;
      }

      .footer-bottom {
        border-top: 1px solid rgba(250, 248, 245, 0.2);
        padding-top: 2rem;
        margin-top: 2rem;
      }

      /* Responsive */
      @media (max-width: 768px) {
        header {
          padding: 1rem;
        }

         nav {
           position: fixed;
           top: 70px;
           left: 0;
           right: 0;
         background: rgba(245, 243, 240, 0.98);
         -webkit-backdrop-filter: blur(10px);
         backdrop-filter: blur(10px);
         display: none;
           flex-direction: column;
           text-align: center;
           border-bottom: 1px solid rgba(139, 69, 19, 0.1);
           padding: 2rem 0;
           box-shadow: 0 4px 20px rgba(139, 69, 19, 0.1);
         }

        nav.active {
          display: flex;
        }

        nav ul {
          flex-direction: column;
          gap: 2rem;
        }

        .hamburger {
          display: block;
        }

        .hero {
          flex-direction: column;
          text-align: center;
          padding: 6rem 5% 4rem;
        }

        .hero h1 {
          font-size: 2.5rem;
        }

        .hero-image img {
          max-width: 300px;
          border-radius: 20px;
          box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
        }

        .hero-buttons {
          justify-content: center;
        }

        .stats {
          gap: 2rem;
        }

        .about {
          flex-direction: column;
          text-align: center;
        }

        .visit-info {
          grid-template-columns: 1fr;
        }

        .products {
          padding: 6rem 5%;
        }

        .products-carousel {
          margin-top: 3rem;
        }

        .carousel-track-wrapper {
          padding: 0 1rem;
        }

        .products-track {
          gap: 2rem;
        }

        .carousel-control {
          width: 46px;
          height: 46px;
        }

        .product-info {
          padding: 1.8rem 1.5rem;
        }

        .product-info h4 {
          font-size: 1.3rem;
        }

        .product-price {
          font-size: 1.6rem;
        }
      }

      @media (max-width: 480px) {
        .hero h1 {
          font-size: 2rem;
        }

        .hero-image img {
          max-width: 250px;
          border-radius: 15px;
          box-shadow: 0 15px 30px rgba(0, 0, 0, 0.2);
        }

        .section-title {
          font-size: 2rem;
        }

        .hero-buttons {
          flex-direction: column;
          align-items: center;
        }

        .btn-primary,
        .btn-secondary {
          width: 100%;
          max-width: 300px;
          justify-content: center;
        }

        .carousel-track-wrapper {
          padding: 0;
        }

        .products-track {
          gap: 1.5rem;
        }

        .carousel-control {
          width: 42px;
          height: 42px;
        }

        .carousel-control.prev {
          left: 0.75rem;
        }

        .carousel-control.next {
          right: 0.75rem;
        }

        .product-info {
          padding: 1.5rem;
        }

        .product-info h4 {
          font-size: 1.2rem;
        }

        .product-price {
          font-size: 1.5rem;
        }
      }
    </style>
  </head>
  <body>
    <?php
    $topbarContext = [
        'rootPrefix' => $rootPrefix,
        'userPrefix' => $userPrefix,
        'imagesBase' => $imagesBase,
        'apiBase' => $rootPrefix . 'PHP/',
    ];
    include __DIR__ . '/UserSide/topbar.php';
    ?>

    <main>
    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-content">
        <div class="hero-badge">
          <i class="fas fa-star"></i> Fresh Daily • Family Owned Since 2008
        </div>
        <h1>Where Every Bite Tells a Story</h1>
        <p>
          Welcome to Cindy's Bakeshop Hagonoy, where tradition meets innovation. 
          Our master bakers craft exceptional breads, exquisite cakes, and delightful 
          pastries using time-honored recipes and the finest ingredients. 
          Experience the authentic taste of home-baked perfection.
        </p>
        <div class="hero-buttons">
          <a href="<?= htmlspecialchars($productBase . 'MENU.php', ENT_QUOTES) ?>" id="exploreMenuBtn" class="btn-primary">
            <i class="fas fa-utensils"></i>
            Explore Menu
          </a>
          <a href="#about" class="btn-secondary">
            <i class="fas fa-info-circle"></i>
            Learn More
          </a>
        </div>
      </div>
      <div class="hero-image">
        <img id="heroImage" src="<?= htmlspecialchars($imagesBase . 'cakes/cake1.png', ENT_QUOTES) ?>" alt="Chocolate Celebration Cake" />
      </div>
    </section>


    <!-- Categories -->
    <section class="categories">
      <div class="section-header">
        <h2 class="section-title">Our Artisan Specialties</h2>
        <p class="section-subtitle">
          From sunrise to sunset, our master bakers create extraordinary baked goods 
          using traditional techniques, premium ingredients, and generations of expertise. 
          Each product tells a story of passion, quality, and community.
        </p>
      </div>
      <div class="categories-grid">
        <div class="category-card">
          <div class="category-icon">
            <i class="fas fa-birthday-cake"></i>
          </div>
          <h3>Signature Cakes</h3>
          <p>
            Masterpieces of flavor and artistry. Our custom cakes are designed to make 
            every celebration unforgettable, from intimate gatherings to grand occasions. 
            Each cake is a work of edible art.
          </p>
        </div>
        <div class="category-card">
          <div class="category-icon">
            <i class="fas fa-bread-slice"></i>
          </div>
          <h3>Artisan Breads</h3>
          <p>
            The foundation of every great meal. Our breads are crafted using traditional 
            fermentation methods, premium flour, and the patience that only comes from 
            generations of baking expertise.
          </p>
        </div>
        <div class="category-card">
          <div class="category-icon">
            <i class="fas fa-cookie-bite"></i>
          </div>
          <h3>Gourmet Pastries</h3>
          <p>
            A symphony of textures and flavors. Our pastries combine French techniques 
            with local ingredients, creating unique treats that delight the senses and 
            create lasting memories.
          </p>
        </div>
      </div>
    </section>

    <!-- Products -->
    <section class="products">
      <div class="section-header">
        <h2 class="section-title">Customer Favorites</h2>
        <p class="section-subtitle">
          Discover the flavors that have made us a beloved part of the Hagonoy community. 
          These signature creations represent the perfect balance of tradition, quality, and innovation.
        </p>
      </div>
      <div class="products-carousel" data-carousel>
        <button class="carousel-control prev" type="button" data-carousel-prev aria-label="View previous favorite">
          <i class="fas fa-chevron-left" aria-hidden="true"></i>
        </button>
        <div class="carousel-track-wrapper">
          <div class="products-track">
            <div class="product-card">
              <div class="product-image">
                <a href="<?= htmlspecialchars($productBase . 'product.php?id=1', ENT_QUOTES) ?>">
                  <img src="<?= htmlspecialchars($imagesBase . 'cakes/cake1.png', ENT_QUOTES) ?>" alt="Choco Mousse Cake" />
                </a>
                <div class="product-badge">Best Seller</div>
              </div>
              <div class="product-info">
                <h4>Choco Mousse Cake</h4>
                <p>Layers of moist chocolate sponge with velvety mousse and a glossy fudge finish.</p>
                <div class="product-price">₱450</div>
              </div>
            </div>
            <div class="product-card">
              <div class="product-image">
                <a href="<?= htmlspecialchars($productBase . 'product.php?id=7', ENT_QUOTES) ?>">
                  <img src="<?= htmlspecialchars($imagesBase . 'cakes/cake2.png', ENT_QUOTES) ?>" alt="Creamy Choco Cake" />
                </a>
                <div class="product-badge">Best Seller</div>
              </div>
              <div class="product-info">
                <h4>Creamy Choco Cake</h4>
                <p>Chocolate sponge layered with whipped cream, chocolate shavings, and silky ganache.</p>
                <div class="product-price">₱420</div>
              </div>
            </div>
            <div class="product-card">
              <div class="product-image">
                <a href="<?= htmlspecialchars($productBase . 'product.php?id=3', ENT_QUOTES) ?>">
                  <img src="<?= htmlspecialchars($imagesBase . 'cakes/cake3.png', ENT_QUOTES) ?>" alt="Choco Cherry Cake" />
                </a>
                <div class="product-badge">New</div>
              </div>
              <div class="product-info">
                <h4>Choco Cherry Cake</h4>
                <p>Dark chocolate cake with cherry compote layers topped with chocolate curls.</p>
                <div class="product-price">₱430</div>
              </div>
            </div>
            <div class="product-card">
              <div class="product-image">
                <a href="<?= htmlspecialchars($productBase . 'product.php?id=4', ENT_QUOTES) ?>">
                  <img src="<?= htmlspecialchars($imagesBase . 'cakes/cake4.png', ENT_QUOTES) ?>" alt="Pastel Delight Cake" />
                </a>
                <div class="product-badge">Best Seller</div>
              </div>
              <div class="product-info">
                <h4>Pastel Delight Cake</h4>
                <p>Classic vanilla chiffon iced with pastel buttercream and candy sprinkles.</p>
                <div class="product-price">₱410</div>
              </div>
            </div>
            <div class="product-card">
              <div class="product-image">
                <a href="<?= htmlspecialchars($productBase . 'product.php?id=5', ENT_QUOTES) ?>">
                  <img src="<?= htmlspecialchars($imagesBase . 'cakes/cake5.png', ENT_QUOTES) ?>" alt="Choco Caramel Cake" />
                </a>
                <div class="product-badge">New</div>
              </div>
              <div class="product-info">
                <h4>Choco Caramel Cake</h4>
                <p>Chocolate sponge layered with caramel cream and finished with caramel drizzle.</p>
                <div class="product-price">₱420</div>
              </div>
            </div>
          </div>
        </div>
        <button class="carousel-control next" type="button" data-carousel-next aria-label="View next favorite">
          <i class="fas fa-chevron-right" aria-hidden="true"></i>
        </button>
      </div>
    </section>

    <!-- About Us -->
    <section class="about" id="about">
      <div class="about-content">
        <h2>Our Story of Passion & Tradition</h2>
        <p>
          For over 15 years, Cindy's Bakeshop Hagonoy has been more than just a bakery – 
          we're a cornerstone of our community. Founded on the principles of family, 
          tradition, and uncompromising quality, we've grown from a small neighborhood 
          bakery into a beloved local institution that serves the entire Hagonoy area.
        </p>
        <p>
          Our journey began with a simple dream: to bring the warmth of home-baked goodness 
          to every family in our community. Today, our master bakers continue this legacy, 
          rising before dawn to create the perfect symphony of flavors that greet you 
          each morning. Every product we create carries the love, care, and expertise 
          that only comes from generations of baking tradition.
        </p>
        <div class="about-features">
          <div class="feature">
            <div class="feature-icon">
              <i class="fas fa-heart"></i>
            </div>
            <div>
              <h4>Handcrafted Excellence</h4>
              <p>Every creation is meticulously crafted by our skilled artisans</p>
            </div>
          </div>
          <div class="feature">
            <div class="feature-icon">
              <i class="fas fa-leaf"></i>
            </div>
            <div>
              <h4>Premium Quality</h4>
              <p>We source only the finest ingredients from trusted suppliers</p>
            </div>
          </div>
          <div class="feature">
            <div class="feature-icon">
              <i class="fas fa-users"></i>
            </div>
            <div>
              <h4>Community First</h4>
              <p>Proudly serving and supporting Hagonoy for over 15 years</p>
            </div>
          </div>
        </div>
      </div>
      <div class="about-image">
        <img src="https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=500&h=400&fit=crop&crop=center" alt="Cindy's Bakeshop Interior" />
      </div>
    </section>

    <!-- Visit Us -->
    <section class="visit" id="visit">
      <div class="section-header">
        <h2 class="section-title">Visit Our Hagonoy Branch</h2>
        <p class="section-subtitle">
          Step into our welcoming space where the aroma of fresh baking fills the air. 
          Experience the warmth of our community and taste the difference that passion makes.
        </p>
      </div>
      <div class="visit-info">
        <div class="info-card">
          <h3>
            <i class="fas fa-map-marker-alt"></i>
            Location
          </h3>
          <p>Halang, Purok 5 San Pedro<br>Hagonoy, Bulacan 3002</p>
        </div>
        <div class="info-card">
          <h3>
            <i class="fas fa-clock"></i>
            Hours
          </h3>
          <p>Monday - Sunday<br>7:00 AM - 9:00 PM</p>
        </div>
        <div class="info-card">
          <h3>
            <i class="fas fa-phone"></i>
            Contact
          </h3>
          <p>(+63) 912-345-6789<br>hagonoy@cindysbakeshop.com</p>
        </div>
      </div>
      <div class="map-container">
        <iframe
          src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3856.7508485268168!2d120.75319130000001!3d14.839235000000002!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3396510044be4d05%3A0xe79baf55cfd3c53b!2sCindy&#39;s%20Bakeshop%20Hagonoy%20Branch!5e0!3m2!1sfil!2sph!4v1757486725570!5m2!1sfil!2sph"
          allowfullscreen=""
          loading="lazy"
          referrerpolicy="no-referrer-when-downgrade"
        ></iframe>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta" id="app">
      <div class="cta-content">
        <h2>Experience Cindy's Anywhere, Anytime</h2>
        <p>
          Join thousands of satisfied customers who enjoy the convenience of ordering 
          our fresh baked goods through our mobile app. Skip the lines, customize your orders, 
          and have your favorites delivered straight to your door.
        </p>
        <a href="#app" class="download-btn">
          <i class="fas fa-mobile-alt"></i>
          Download Our App
        </a>
      </div>
    </section>

    </main>

    <!-- Footer -->
    <footer>
      <div class="footer-content">
        <div class="footer-section">
          <h3>Cindy's Bakeshop Hagonoy</h3>
          <p>
            Where tradition meets innovation. We've been crafting exceptional baked goods 
            with love, passion, and the finest ingredients for over 15 years. 
            Your neighborhood bakery, your family's favorite.
          </p>
        </div>
        <div class="footer-section">
          <h3>Quick Links</h3>
          <p><a href="<?= htmlspecialchars($productBase . 'MENU.php', ENT_QUOTES) ?>" id="footerMenuLink">Our Menu</a></p>
          <p><a href="#about">Our Story</a></p>
          <p><a href="#visit">Visit Us</a></p>
          <p><a href="#app">Download App</a></p>
        </div>
        <div class="footer-section">
          <h3>Contact Info</h3>
          <p>Halang, Purok 5 San Pedro</p>
          <p>Hagonoy, Bulacan 3002</p>
          <p>(+63) 912-345-6789</p>
        </div>
        <div class="footer-section">
          <h3>Follow Us</h3>
          <p><a href="#">Facebook</a></p>
          <p><a href="#">Instagram</a></p>
          <p><a href="#">Twitter</a></p>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2025 Cindy's Bakeshop Hagonoy. All rights reserved.</p>
      </div>
    </footer>

    <script>
      document.addEventListener('DOMContentLoaded', () => {
        const smoothLinks = document.querySelectorAll('a[href^="#"]');
        smoothLinks.forEach(link => {
          link.addEventListener('click', (event) => {
            const targetId = link.getAttribute('href');
            if (targetId && targetId.length > 1) {
              const target = document.querySelector(targetId);
              if (target) {
                event.preventDefault();
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
              }
            }
          });
        });

        const observer = new IntersectionObserver((entries, obs) => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              entry.target.style.opacity = '1';
              entry.target.style.transform = 'translateY(0)';
              obs.unobserve(entry.target);
            }
          });
        }, { threshold: 0.1, rootMargin: '0px 0px -50px 0px' });

        document.querySelectorAll('.category-card, .product-card, .info-card, .cta').forEach(element => {
          element.style.opacity = '0';
          element.style.transform = 'translateY(30px)';
          element.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
          observer.observe(element);
        });

        const carousels = document.querySelectorAll('[data-carousel]');
        carousels.forEach(carousel => {
          const track = carousel.querySelector('.products-track');
          const wrapper = carousel.querySelector('.carousel-track-wrapper');
          const prevButton = carousel.querySelector('[data-carousel-prev]');
          const nextButton = carousel.querySelector('[data-carousel-next]');
          if (!track || !wrapper) {
            return;
          }

          const cards = Array.from(track.children);
          if (cards.length <= 1) {
            if (prevButton) {
              prevButton.hidden = true;
            }
            if (nextButton) {
              nextButton.hidden = true;
            }
            return;
          }

          cards.forEach(card => {
            card.style.flex = '0 0 100%';
          });

          let isAnimating = false;
          let autoTimer;
          const motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)');
          const shouldAutoplay = () => !motionPreference.matches;

          const enableTransition = () => {
            track.style.transition = 'transform 0.6s ease';
          };

          const disableTransition = () => {
            track.style.transition = 'none';
          };

          const slideDistance = () => wrapper.getBoundingClientRect().width;

          const stopAutoplay = () => {
            if (autoTimer) {
              window.clearInterval(autoTimer);
              autoTimer = undefined;
            }
          };

          const startAutoplay = () => {
            if (!shouldAutoplay()) {
              stopAutoplay();
              return;
            }
            stopAutoplay();
            autoTimer = window.setInterval(() => {
              moveNext();
            }, 6000);
          };

          const resetAutoplay = () => {
            if (shouldAutoplay()) {
              stopAutoplay();
              startAutoplay();
            } else {
              stopAutoplay();
            }
          };

          const moveNext = () => {
            if (isAnimating) {
              return;
            }
            isAnimating = true;
            enableTransition();
            const distance = slideDistance();
            track.style.transform = `translateX(-${distance}px)`;

            const handleTransitionEnd = () => {
              track.removeEventListener('transitionend', handleTransitionEnd);
              disableTransition();
              track.appendChild(track.firstElementChild);
              track.style.transform = 'translateX(0)';
              void track.offsetWidth;
              enableTransition();
              isAnimating = false;
            };

            track.addEventListener('transitionend', handleTransitionEnd, { once: true });
          };

          const movePrev = () => {
            if (isAnimating) {
              return;
            }
            isAnimating = true;
            const distance = slideDistance();
            disableTransition();
            track.insertBefore(track.lastElementChild, track.firstElementChild);
            track.style.transform = `translateX(-${distance}px)`;
            void track.offsetWidth;
            enableTransition();
            track.style.transform = 'translateX(0)';

            const handleTransitionEnd = () => {
              track.removeEventListener('transitionend', handleTransitionEnd);
              isAnimating = false;
            };

            track.addEventListener('transitionend', handleTransitionEnd, { once: true });
          };

          if (nextButton) {
            nextButton.addEventListener('click', () => {
              moveNext();
              resetAutoplay();
            });
          }

          if (prevButton) {
            prevButton.addEventListener('click', () => {
              movePrev();
              resetAutoplay();
            });
          }

          carousel.addEventListener('mouseenter', stopAutoplay);
          carousel.addEventListener('mouseleave', () => {
            if (shouldAutoplay()) {
              startAutoplay();
            }
          });
          carousel.addEventListener('focusin', stopAutoplay);
          carousel.addEventListener('focusout', () => {
            if (shouldAutoplay()) {
              startAutoplay();
            }
          });
          carousel.addEventListener('touchstart', stopAutoplay, { passive: true });
          carousel.addEventListener('touchend', () => {
            if (shouldAutoplay()) {
              startAutoplay();
            }
          }, { passive: true });

          window.addEventListener('resize', () => {
            disableTransition();
            track.style.transform = 'translateX(0)';
            void track.offsetWidth;
            enableTransition();
          });

          const handleMotionChange = () => {
            if (!shouldAutoplay()) {
              stopAutoplay();
            } else {
              startAutoplay();
            }
          };

          if (typeof motionPreference.addEventListener === 'function') {
            motionPreference.addEventListener('change', handleMotionChange);
          } else if (typeof motionPreference.addListener === 'function') {
            motionPreference.addListener(handleMotionChange);
          }

          if (shouldAutoplay()) {
            startAutoplay();
          }
        });

        const heroImages = [
          {
            src: '<?= htmlspecialchars($imagesBase . 'cakes/cake1.png', ENT_QUOTES) ?>',
            alt: 'Chocolate Celebration Cake'
          },
          {
            src: '<?= htmlspecialchars($imagesBase . 'bread/bread10.png', ENT_QUOTES) ?>',
            alt: 'Whole Wheat Loaf'
          },
          {
            src: '<?= htmlspecialchars($imagesBase . 'pastry/pastry6.png', ENT_QUOTES) ?>',
            alt: 'Brownie Bites'
          },
          {
            src: '<?= htmlspecialchars($imagesBase . 'pastry/pastry10.png', ENT_QUOTES) ?>',
            alt: "Snap'n Roll"
          },
          {
            src: '<?= htmlspecialchars($imagesBase . 'cakes/cake15.png', ENT_QUOTES) ?>',
            alt: 'Mocha Celebration Cake'
          }
        ];

        const heroImage = document.getElementById('heroImage');
        if (heroImage && heroImages.length > 1) {
          let currentImageIndex = 0;
          setInterval(() => {
            currentImageIndex = (currentImageIndex + 1) % heroImages.length;
            const nextImage = heroImages[currentImageIndex];
            heroImage.style.opacity = '0.7';
            heroImage.style.transform = 'scale(0.95)';
            setTimeout(() => {
              heroImage.src = nextImage.src;
              heroImage.alt = nextImage.alt;
              heroImage.style.opacity = '1';
              heroImage.style.transform = 'scale(1)';
            }, 300);
          }, 5000);
        }
      });
    </script>
  </body>
</html>
