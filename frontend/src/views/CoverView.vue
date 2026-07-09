<script setup lang="ts">
import { useRouter } from 'vue-router'
import coverBg from '../assets/campus-cover-bg.jpg'

const router = useRouter()

function enterHome() {
  router.push('/home')
}
</script>

<template>
  <section class="cover-page" :style="{ backgroundImage: `url(${coverBg})` }" aria-label="平台入口封面">
    <div class="cover-layer"></div>
    <div class="cover-content">
      <p class="cover-kicker">校园二手交易平台</p>
      <h1>同校面对面 交易更安全</h1>
      <p class="cover-subtitle">校内闲置流转，发布、沟通、下单与审核一站完成。</p>
      <button class="enter-button" type="button" @click="enterHome">
        进入平台
        <span aria-hidden="true"></span>
      </button>
    </div>
    <div class="cover-foot">
      <span>商品审核</span>
      <span>智能推荐</span>
      <span>纠纷处理</span>
    </div>
  </section>
</template>

<style scoped>
.cover-page {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 100vh;
  overflow: hidden;
  background-position: center;
  background-size: cover;
  isolation: isolate;
}

.cover-layer {
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.24) 0%, rgba(255, 255, 255, 0.1) 42%, rgba(15, 23, 42, 0.24) 100%),
    linear-gradient(90deg, rgba(15, 23, 42, 0.36) 0%, rgba(15, 23, 42, 0.06) 48%, rgba(15, 23, 42, 0.24) 100%);
  backdrop-filter: saturate(1.04);
}

.cover-content {
  display: grid;
  justify-items: center;
  width: min(860px, calc(100% - 40px));
  margin-top: -8vh;
  padding: 34px 36px 38px;
  border: 1px solid rgba(255, 255, 255, 0.66);
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.2), rgba(15, 23, 42, 0.1));
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.22);
  text-align: center;
  backdrop-filter: blur(8px);
  animation: cover-rise 0.7s ease both;
}

.cover-kicker {
  margin: 0 0 12px;
  padding: 7px 14px;
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 999px;
  color: #eff6ff;
  background: rgba(22, 119, 255, 0.28);
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

h1 {
  margin: 0;
  color: #fff;
  font-size: clamp(42px, 6vw, 78px);
  line-height: 1.08;
  font-weight: 950;
  letter-spacing: 0;
  text-shadow: 0 4px 20px rgba(15, 23, 42, 0.34);
}

.cover-subtitle {
  margin: 18px 0 42px;
  max-width: 560px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 18px;
  line-height: 1.7;
  text-shadow: 0 2px 12px rgba(15, 23, 42, 0.28);
}

.enter-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 190px;
  height: 58px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 999px;
  color: #fff;
  background: linear-gradient(135deg, #1677ff, #22a8ff);
  box-shadow: 0 18px 38px rgba(22, 119, 255, 0.34);
  cursor: pointer;
  font-size: 20px;
  font-weight: 950;
  letter-spacing: 0.08em;
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.enter-button span {
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 0%, rgba(255, 255, 255, 0.36) 44%, transparent 70%);
  opacity: 0;
  transform: translateX(-60%);
  transition: opacity 0.2s ease, transform 0.45s ease;
}

.enter-button:hover {
  filter: saturate(1.08);
  box-shadow: 0 22px 46px rgba(22, 119, 255, 0.42);
  transform: translateY(-2px);
}

.enter-button:hover span {
  opacity: 1;
  transform: translateX(58%);
}

.cover-foot {
  position: absolute;
  right: 28px;
  bottom: 24px;
  left: 28px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.cover-foot span {
  min-height: 34px;
  padding: 8px 14px;
  border: 1px solid rgba(255, 255, 255, 0.56);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(15, 23, 42, 0.2);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  font-size: 13px;
  font-weight: 900;
  backdrop-filter: blur(8px);
}

@keyframes cover-rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 720px) {
  .cover-content {
    margin-top: -4vh;
    padding: 26px 20px 30px;
  }

  .cover-subtitle {
    margin-bottom: 30px;
    font-size: 15px;
  }

  .enter-button {
    width: min(240px, 100%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .cover-content {
    animation: none;
  }

  .enter-button,
  .enter-button span {
    transition: none;
  }
}
</style>
